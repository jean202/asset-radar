package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.StatisticsCalculator;
import com.jean202.assetradar.analysis.SummaryStatistics;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetStatisticsReader;
import com.jean202.assetradar.query.CorrelationQuery;
import com.jean202.assetradar.query.StatisticsQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/statistics")
public class AssetStatisticsController {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(?i)^(\\d+)([SMHDW])$");

    private final AssetStatisticsReader reader;
    private final StatisticsCalculator calculator;
    private final Clock clock;

    @Autowired
    public AssetStatisticsController(AssetStatisticsReader reader, StatisticsCalculator calculator) {
        this(reader, calculator, Clock.systemUTC());
    }

    AssetStatisticsController(AssetStatisticsReader reader, StatisticsCalculator calculator, Clock clock) {
        this.reader = reader;
        this.calculator = calculator;
        this.clock = clock;
    }

    @GetMapping("/moving-average")
    public Mono<MovingAverageResponse> movingAverage(
            @RequestParam String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(defaultValue = "SMA") String type,
            @RequestParam(defaultValue = "20") int window,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(resolvePeriod(period)) : from;
        StatisticsQuery query = new StatisticsQuery(symbol, source, quoteCurrency, resolvedFrom, resolvedTo);

        return reader.readPriceWindow(query)
                .collectList()
                .map(prices -> buildMovingAverageResponse(query, prices, type.toUpperCase(), window));
    }

    @GetMapping("/volatility")
    public Mono<VolatilityResponse> volatility(
            @RequestParam String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(defaultValue = "20") int window,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(resolvePeriod(period)) : from;
        StatisticsQuery query = new StatisticsQuery(symbol, source, quoteCurrency, resolvedFrom, resolvedTo);

        return reader.readPriceWindow(query)
                .collectList()
                .map(prices -> buildVolatilityResponse(query, prices, window));
    }

    @GetMapping("/correlation")
    public Mono<CorrelationResponse> correlation(
            @RequestParam List<String> assets,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(resolvePeriod(period)) : from;

        CorrelationQuery query = new CorrelationQuery(assets, source, quoteCurrency, resolvedFrom, resolvedTo);

        return Flux.fromIterable(query.assets())
                .flatMap(spec -> {
                    StatisticsQuery sq = new StatisticsQuery(
                            spec.symbol(), spec.source(), spec.quoteCurrency(),
                            query.from(), query.to());
                    return reader.readPriceWindow(sq)
                            .collectList()
                            .map(prices -> Map.entry(spec, prices));
                })
                .collectList()
                .map(entries -> buildCorrelationResponse(query, entries));
    }

    @GetMapping("/summary")
    public Mono<SummaryResponse> summary(
            @RequestParam String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(resolvePeriod(period)) : from;
        StatisticsQuery query = new StatisticsQuery(symbol, source, quoteCurrency, resolvedFrom, resolvedTo);

        return reader.readPriceWindow(query)
                .collectList()
                .map(prices -> buildSummaryResponse(query, prices));
    }

    private MovingAverageResponse buildMovingAverageResponse(
            StatisticsQuery query, List<AssetPrice> prices, String type, int window
    ) {
        List<BigDecimal> priceValues = prices.stream().map(AssetPrice::price).toList();
        List<BigDecimal> maValues = "EMA".equals(type)
                ? calculator.ema(priceValues, window)
                : calculator.sma(priceValues, window);

        int offset = "EMA".equals(type) ? 0 : window - 1;
        List<MovingAverageResponse.MovingAveragePoint> points = new ArrayList<>();
        for (int i = 0; i < maValues.size(); i++) {
            int priceIdx = i + offset;
            if (priceIdx < prices.size()) {
                points.add(new MovingAverageResponse.MovingAveragePoint(
                        prices.get(priceIdx).collectedAt(),
                        prices.get(priceIdx).price(),
                        maValues.get(i)
                ));
            }
        }

        String resolvedSource = prices.isEmpty() ? query.source() : prices.get(0).source();
        String resolvedCurrency = prices.isEmpty() ? query.quoteCurrency() : prices.get(0).quoteCurrency();

        return new MovingAverageResponse(
                query.symbol(), resolvedSource, resolvedCurrency,
                query.from(), query.to(),
                type, window, points.size(), points
        );
    }

    private VolatilityResponse buildVolatilityResponse(
            StatisticsQuery query, List<AssetPrice> prices, int window
    ) {
        List<BigDecimal> priceValues = prices.stream().map(AssetPrice::price).toList();
        List<BigDecimal> returnValues = calculator.returns(priceValues);
        List<BigDecimal> rollingVol = calculator.rollingStdDev(returnValues, window);

        int offset = window;
        List<VolatilityResponse.VolatilityPoint> points = new ArrayList<>();
        for (int i = 0; i < rollingVol.size(); i++) {
            int priceIdx = i + offset;
            if (priceIdx < prices.size()) {
                points.add(new VolatilityResponse.VolatilityPoint(
                        prices.get(priceIdx).collectedAt(),
                        rollingVol.get(i)
                ));
            }
        }

        BigDecimal currentVol = rollingVol.isEmpty() ? BigDecimal.ZERO : rollingVol.get(rollingVol.size() - 1);
        BigDecimal annualized = currentVol.multiply(BigDecimal.valueOf(Math.sqrt(365)))
                .setScale(8, RoundingMode.HALF_UP);

        String resolvedSource = prices.isEmpty() ? query.source() : prices.get(0).source();
        String resolvedCurrency = prices.isEmpty() ? query.quoteCurrency() : prices.get(0).quoteCurrency();

        return new VolatilityResponse(
                query.symbol(), resolvedSource, resolvedCurrency,
                query.from(), query.to(),
                window, points.size(),
                currentVol, annualized, points
        );
    }

    private CorrelationResponse buildCorrelationResponse(
            CorrelationQuery query,
            List<Map.Entry<AssetCompareQuery.AssetSpec, List<AssetPrice>>> entries
    ) {
        // Build timestamp-aligned return maps per asset
        Map<AssetCompareQuery.AssetSpec, Map<Instant, BigDecimal>> returnMaps = new LinkedHashMap<>();
        for (Map.Entry<AssetCompareQuery.AssetSpec, List<AssetPrice>> entry : entries) {
            List<AssetPrice> prices = entry.getValue();
            Map<Instant, BigDecimal> returnMap = new LinkedHashMap<>();
            for (int i = 1; i < prices.size(); i++) {
                BigDecimal prev = prices.get(i - 1).price();
                if (prev.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal ret = prices.get(i).price().subtract(prev)
                            .divide(prev, 8, RoundingMode.HALF_UP);
                    returnMap.put(prices.get(i).collectedAt(), ret);
                }
            }
            returnMaps.put(entry.getKey(), returnMap);
        }

        List<AssetCompareQuery.AssetSpec> specs = new ArrayList<>(returnMaps.keySet());
        List<CorrelationResponse.CorrelationPair> pairs = new ArrayList<>();

        for (int i = 0; i < specs.size(); i++) {
            for (int j = i + 1; j < specs.size(); j++) {
                AssetCompareQuery.AssetSpec a = specs.get(i);
                AssetCompareQuery.AssetSpec b = specs.get(j);

                // Find overlapping timestamps
                Map<Instant, BigDecimal> mapA = returnMaps.get(a);
                Map<Instant, BigDecimal> mapB = returnMaps.get(b);

                List<BigDecimal> alignedA = new ArrayList<>();
                List<BigDecimal> alignedB = new ArrayList<>();
                for (Map.Entry<Instant, BigDecimal> e : mapA.entrySet()) {
                    BigDecimal valB = mapB.get(e.getKey());
                    if (valB != null) {
                        alignedA.add(e.getValue());
                        alignedB.add(valB);
                    }
                }

                BigDecimal corr = calculator.pearsonCorrelation(alignedA, alignedB);
                pairs.add(new CorrelationResponse.CorrelationPair(
                        a.symbol(), a.source(),
                        b.symbol(), b.source(),
                        corr, alignedA.size()
                ));
            }
        }

        return new CorrelationResponse(query.from(), query.to(), specs.size(), pairs);
    }

    private SummaryResponse buildSummaryResponse(StatisticsQuery query, List<AssetPrice> prices) {
        List<BigDecimal> priceValues = prices.stream().map(AssetPrice::price).toList();
        SummaryStatistics stats = calculator.summarize(priceValues);
        BigDecimal maxDd = calculator.maxDrawdown(priceValues);

        BigDecimal latestPrice = prices.isEmpty() ? BigDecimal.ZERO : prices.get(prices.size() - 1).price();
        BigDecimal firstPrice = prices.isEmpty() ? BigDecimal.ZERO : prices.get(0).price();
        BigDecimal returnRate = firstPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : latestPrice.subtract(firstPrice).divide(firstPrice, 8, RoundingMode.HALF_UP);

        String resolvedSource = prices.isEmpty() ? query.source() : prices.get(0).source();
        String resolvedCurrency = prices.isEmpty() ? query.quoteCurrency() : prices.get(0).quoteCurrency();

        return new SummaryResponse(
                query.symbol(), resolvedSource, resolvedCurrency,
                query.from(), query.to(),
                stats.count(), stats.min(), stats.max(), stats.mean(), stats.stdDev(),
                stats.median(), stats.p5(), stats.p25(), stats.p75(), stats.p95(),
                latestPrice, returnRate, maxDd
        );
    }

    private Duration resolvePeriod(String period) {
        String rawValue = period == null || period.isBlank() ? "30D" : period.trim();
        Matcher matcher = PERIOD_PATTERN.matcher(rawValue);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("period must be one of Ns, Nm, Nh, Nd, Nw");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2).toUpperCase()) {
            case "S" -> Duration.ofSeconds(amount);
            case "M" -> Duration.ofMinutes(amount);
            case "H" -> Duration.ofHours(amount);
            case "D" -> Duration.ofDays(amount);
            case "W" -> Duration.ofDays(amount * 7);
            default -> throw new IllegalArgumentException("period must be one of Ns, Nm, Nh, Nd, Nw");
        };
    }
}
