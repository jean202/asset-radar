package com.jean202.assetradar.api;

import com.jean202.assetradar.config.DashboardProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceStore;
import com.jean202.assetradar.query.DashboardHistoryMetrics;
import com.jean202.assetradar.query.DashboardHistoryMetricsReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final Map<String, SourceMetadata> SOURCE_METADATA = Map.of(
            "UPBIT", new SourceMetadata("업비트", "COIN", "코인"),
            "BINANCE", new SourceMetadata("바이낸스", "COIN", "코인"),
            "GOLDAPI", new SourceMetadata("Gold API", "GOLD", "금"),
            "KIS", new SourceMetadata("한국투자증권", "STOCK_KR", "국내주식"),
            "KOREAINVESTMENT", new SourceMetadata("한국투자증권", "STOCK_KR", "국내주식"),
            "ALPHAVANTAGE", new SourceMetadata("Alpha Vantage", "STOCK_US", "미국주식"),
            "FINNHUB", new SourceMetadata("Finnhub", "STOCK_US", "미국주식")
    );
    private static final SourceMetadata UNKNOWN_SOURCE_METADATA = new SourceMetadata("Unknown Source", "UNKNOWN", "기타");

    private static final Comparator<AssetPrice> DASHBOARD_ASSET_ORDER = Comparator
            .comparing(AssetPrice::source, Comparator.nullsLast(String::compareTo))
            .thenComparing(AssetPrice::quoteCurrency, Comparator.nullsLast(String::compareTo))
            .thenComparing(AssetPrice::symbol, Comparator.nullsLast(String::compareTo))
            .thenComparing(AssetPrice::collectedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final AssetPriceStore assetPriceStore;
    private final LatestAssetPriceReader latestAssetPriceReader;
    private final DashboardHistoryMetricsReader dashboardHistoryMetricsReader;
    private final DashboardProperties dashboardProperties;
    private final Clock clock;

    @Autowired
    public DashboardController(
            AssetPriceStore assetPriceStore,
            LatestAssetPriceReader latestAssetPriceReader,
            DashboardHistoryMetricsReader dashboardHistoryMetricsReader,
            DashboardProperties dashboardProperties
    ) {
        this(
                assetPriceStore,
                latestAssetPriceReader,
                dashboardHistoryMetricsReader,
                dashboardProperties,
                Clock.systemUTC()
        );
    }

    DashboardController(
            AssetPriceStore assetPriceStore,
            LatestAssetPriceReader latestAssetPriceReader,
            DashboardHistoryMetricsReader dashboardHistoryMetricsReader,
            DashboardProperties dashboardProperties,
            Clock clock
    ) {
        this.assetPriceStore = assetPriceStore;
        this.latestAssetPriceReader = latestAssetPriceReader;
        this.dashboardHistoryMetricsReader = dashboardHistoryMetricsReader;
        this.dashboardProperties = dashboardProperties;
        this.clock = clock;
    }

    @GetMapping
    public Mono<DashboardResponse> dashboard() {
        Mono<List<AssetPrice>> latestAssets = latestAssetPriceReader.readLatest(new LatestAssetQuery(null, null, List.of()))
                .collectList();
        Mono<DashboardHistoryMetrics> historyMetrics = dashboardHistoryMetricsReader.readMetrics();

        return Mono.zip(latestAssets, historyMetrics)
                .map(tuple -> toDashboardResponse(tuple.getT1(), tuple.getT2()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AssetPrice>> stream() {
        return Flux.fromIterable(assetPriceStore.snapshot())
                .concatWith(assetPriceStore.stream())
                .map(this::toServerSentEvent);
    }

    private ServerSentEvent<AssetPrice> toServerSentEvent(AssetPrice price) {
        return ServerSentEvent.<AssetPrice>builder(price)
                .event("asset-price")
                .id("%s-%s-%s-%d".formatted(
                        price.source(),
                        price.quoteCurrency(),
                        price.symbol(),
                        price.collectedAt().toEpochMilli()
                ))
                .build();
    }

    private DashboardResponse toDashboardResponse(List<AssetPrice> assets, DashboardHistoryMetrics historyMetrics) {
        List<AssetPrice> sortedAssets = assets.stream()
                .sorted(DASHBOARD_ASSET_ORDER)
                .toList();

        Instant updatedAt = sortedAssets.stream()
                .map(AssetPrice::collectedAt)
                .max(Instant::compareTo)
                .orElse(historyMetrics.latestCollectedAt());
        List<DashboardSourceGroup> sourceGroups = toSourceGroups(sortedAssets);

        return new DashboardResponse(
                resolveStatus(sortedAssets, historyMetrics),
                updatedAt,
                sortedAssets.size(),
                historyMetrics.historyRowCount(),
                historyMetrics.latestCollectedAt(),
                sortedAssets,
                sourceGroups
        );
    }

    private String resolveStatus(List<AssetPrice> assets, DashboardHistoryMetrics historyMetrics) {
        if (!assets.isEmpty()) {
            return "live";
        }

        return Optional.ofNullable(historyMetrics.latestCollectedAt())
                .map(ignored -> "degraded")
                .orElse("warming-up");
    }

    private List<DashboardSourceGroup> toSourceGroups(List<AssetPrice> assets) {
        Map<String, List<AssetPrice>> grouped = assets.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AssetPrice::source,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> toSourceGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    private DashboardSourceGroup toSourceGroup(String source, List<AssetPrice> assets) {
        SourceMetadata metadata = sourceMetadataOf(source);
        Instant updatedAt = assets.stream()
                .map(AssetPrice::collectedAt)
                .max(Instant::compareTo)
                .orElse(null);
        long lastUpdatedAgeSeconds = updatedAt == null
                ? 0
                : Math.max(0, Duration.between(updatedAt, Instant.now(clock)).getSeconds());
        Duration staleThreshold = dashboardProperties.staleThresholdFor(source);

        return new DashboardSourceGroup(
                source,
                metadata.sourceDisplayName(),
                metadata.assetType(),
                metadata.assetTypeLabel(),
                updatedAt,
                updatedAt == null || lastUpdatedAgeSeconds > staleThreshold.getSeconds(),
                lastUpdatedAgeSeconds,
                updatedAt == null ? "unknown" : formatAgo(lastUpdatedAgeSeconds),
                assets.size(),
                List.copyOf(assets)
        );
    }

    private SourceMetadata sourceMetadataOf(String source) {
        if (source == null) {
            return UNKNOWN_SOURCE_METADATA;
        }

        return SOURCE_METADATA.getOrDefault(source, new SourceMetadata(source, "UNKNOWN", "기타"));
    }

    private String formatAgo(long ageSeconds) {
        if (ageSeconds < 60) {
            return "%ds ago".formatted(ageSeconds);
        }

        if (ageSeconds < 3600) {
            return "%dm ago".formatted(ageSeconds / 60);
        }

        if (ageSeconds < 86400) {
            return "%dh ago".formatted(ageSeconds / 3600);
        }

        return "%dd ago".formatted(ageSeconds / 86400);
    }

    private record SourceMetadata(
            String sourceDisplayName,
            String assetType,
            String assetTypeLabel
    ) {
    }
}
