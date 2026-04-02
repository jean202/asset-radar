package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.AssetComparator;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
@RequestMapping("/api/compare")
public class AssetCompareController {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(?i)^(\\d+)([SMHDW])$");
    private static final Comparator<ComparedAssetResponse> COMPARE_ORDER = Comparator
            .comparing(ComparedAssetResponse::returnRate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ComparedAssetResponse::symbol, Comparator.nullsLast(String::compareTo))
            .thenComparing(ComparedAssetResponse::source, Comparator.nullsLast(String::compareTo));

    private final AssetCompareWindowReader assetCompareWindowReader;
    private final AssetComparator assetComparator;
    private final Clock clock;

    @Autowired
    public AssetCompareController(
            AssetCompareWindowReader assetCompareWindowReader,
            AssetComparator assetComparator
    ) {
        this(assetCompareWindowReader, assetComparator, Clock.systemUTC());
    }

    AssetCompareController(
            AssetCompareWindowReader assetCompareWindowReader,
            AssetComparator assetComparator,
            Clock clock
    ) {
        this.assetCompareWindowReader = assetCompareWindowReader;
        this.assetComparator = assetComparator;
        this.clock = clock;
    }

    @GetMapping
    public Mono<CompareResponse> compare(
            @RequestParam List<String> assets,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal baseAmount
    ) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(resolvePeriod(period)) : from;
        AssetCompareQuery query = new AssetCompareQuery(
                assets,
                source,
                quoteCurrency,
                resolvedFrom,
                resolvedTo,
                baseAmount
        );

        return Flux.fromIterable(query.assets())
                .flatMap(asset -> assetCompareWindowReader.readWindow(asset, query.from(), query.to())
                        .map(window -> assetComparator.compare(window, query.baseAmount()))
                        .filter(Objects::nonNull))
                .sort(COMPARE_ORDER)
                .collectList()
                .map(comparisons -> toCompareResponse(query, comparisons));
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

    private CompareResponse toCompareResponse(AssetCompareQuery query, List<ComparedAssetResponse> comparisons) {
        List<String> quoteCurrencies = comparisons.stream()
                .map(ComparedAssetResponse::quoteCurrency)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        boolean projectedValueComparable = quoteCurrencies.size() <= 1;
        List<ComparedAssetResponse> adjustedComparisons = projectedValueComparable
                ? comparisons
                : comparisons.stream()
                        .map(ComparedAssetResponse::withoutProjectedValue)
                        .toList();

        return new CompareResponse(
                query.from(),
                query.to(),
                query.baseAmount(),
                query.assets().size(),
                adjustedComparisons.size(),
                projectedValueComparable,
                quoteCurrencies,
                projectedValueComparable ? null : "projectedValue is only comparable when all assets share the same quoteCurrency",
                adjustedComparisons
        );
    }
}
