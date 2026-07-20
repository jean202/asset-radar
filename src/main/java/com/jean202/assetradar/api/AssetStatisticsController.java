package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.StatisticsCalculator;
import com.jean202.assetradar.analysis.SummaryStatistics;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetStatisticsReader;
import com.jean202.assetradar.query.CorrelationQuery;
import com.jean202.assetradar.query.StatisticsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "이동평균, 변동성, 상관관계, 요약 통계")
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
    @Operation(summary = "이동평균 조회", description = "가격 이력으로 SMA 또는 EMA 이동평균 시계열을 계산합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "이동평균 결과",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MovingAverageResponse.class),
                    examples = @ExampleObject(name = "moving-average", value = OpenApiExamples.MOVING_AVERAGE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 기간, 날짜 또는 query param",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-period", value = OpenApiExamples.ERROR_INVALID_STATISTICS_PERIOD)
            )
    )
    public Mono<MovingAverageResponse> movingAverage(
            @Parameter(description = "자산 심볼", example = "BTC", required = true)
            @RequestParam String symbol,
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "이동평균 타입. SMA 또는 EMA", example = "SMA")
            @RequestParam(defaultValue = "SMA") String type,
            @Parameter(description = "계산 윈도우 크기", example = "20")
            @RequestParam(defaultValue = "20") int window,
            @Parameter(description = "조회 기간. 예: 30d, 12h, 15m", example = "30d")
            @RequestParam(required = false) String period,
            @Parameter(description = "명시적 시작 시각, ISO-8601", example = "2026-04-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "명시적 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
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
    @Operation(summary = "변동성 조회", description = "가격 이력 수익률의 rolling standard deviation과 연율화 변동성을 계산합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "변동성 결과",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = VolatilityResponse.class),
                    examples = @ExampleObject(name = "volatility", value = OpenApiExamples.VOLATILITY)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 기간, 날짜 또는 query param",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-period", value = OpenApiExamples.ERROR_INVALID_STATISTICS_PERIOD)
            )
    )
    public Mono<VolatilityResponse> volatility(
            @Parameter(description = "자산 심볼", example = "BTC", required = true)
            @RequestParam String symbol,
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "rolling volatility 윈도우 크기", example = "20")
            @RequestParam(defaultValue = "20") int window,
            @Parameter(description = "조회 기간. 예: 30d, 12h, 15m", example = "30d")
            @RequestParam(required = false) String period,
            @Parameter(description = "명시적 시작 시각, ISO-8601", example = "2026-04-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "명시적 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
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
    @Operation(summary = "상관관계 조회", description = "여러 자산의 수익률을 timestamp 기준으로 정렬해 Pearson correlation을 계산합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "상관관계 결과",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CorrelationResponse.class),
                    examples = @ExampleObject(name = "correlation", value = OpenApiExamples.CORRELATION)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 assets, 기간, 날짜 또는 query param",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-period", value = OpenApiExamples.ERROR_INVALID_STATISTICS_PERIOD)
            )
    )
    public Mono<CorrelationResponse> correlation(
            @Parameter(description = "`SYMBOL` 또는 `SOURCE:QUOTE:SYMBOL` 형식의 자산 목록", example = "UPBIT:KRW:BTC,UPBIT:KRW:ETH", required = true)
            @RequestParam List<String> assets,
            @Parameter(description = "assets가 SYMBOL만 포함할 때 적용할 기본 source", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "assets가 SYMBOL만 포함할 때 적용할 기본 quoteCurrency", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회 기간. 예: 30d, 12h, 15m", example = "30d")
            @RequestParam(required = false) String period,
            @Parameter(description = "명시적 시작 시각, ISO-8601", example = "2026-04-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "명시적 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
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
    @Operation(summary = "요약 통계 조회", description = "가격 이력의 min, max, mean, percentile, 수익률, max drawdown을 계산합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "요약 통계",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SummaryResponse.class),
                    examples = @ExampleObject(name = "summary", value = OpenApiExamples.SUMMARY)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 기간, 날짜 또는 query param",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-period", value = OpenApiExamples.ERROR_INVALID_STATISTICS_PERIOD)
            )
    )
    public Mono<SummaryResponse> summary(
            @Parameter(description = "자산 심볼", example = "BTC", required = true)
            @RequestParam String symbol,
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회 기간. 예: 30d, 12h, 15m", example = "30d")
            @RequestParam(required = false) String period,
            @Parameter(description = "명시적 시작 시각, ISO-8601", example = "2026-04-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "명시적 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
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
