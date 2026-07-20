package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.AssetComparator;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/compare")
@Tag(name = "Compare", description = "여러 자산의 기간 수익률과 동일 금액 투자 결과 비교")
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
    @Operation(
            summary = "자산 수익률 비교",
            description = "`assets`는 `SYMBOL` 또는 `SOURCE:QUOTE:SYMBOL` 형식을 받으며, 서로 다른 quoteCurrency가 섞이면 projectedValue는 null로 내려갑니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "자산 비교 결과",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CompareResponse.class),
                    examples = @ExampleObject(name = "mixed-currency-compare", value = OpenApiExamples.COMPARE)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 assets, period 또는 baseAmount",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-period", value = OpenApiExamples.ERROR_INVALID_PERIOD)
            )
    )
    public Mono<CompareResponse> compare(
            @Parameter(description = "`SYMBOL` 또는 `SOURCE:QUOTE:SYMBOL` 형식의 비교 대상 목록", example = "UPBIT:KRW:BTC,GOLDAPI:USD:XAU", required = true)
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "동일 금액 투자 결과 계산 기준 금액", example = "1000000")
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
