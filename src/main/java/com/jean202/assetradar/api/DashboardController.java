package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceStore;
import com.jean202.assetradar.query.DashboardHistoryMetrics;
import com.jean202.assetradar.query.DashboardHistoryMetricsReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    private final AssetPriceStore assetPriceStore;
    private final LatestAssetPriceReader latestAssetPriceReader;
    private final DashboardHistoryMetricsReader dashboardHistoryMetricsReader;

    public DashboardController(
            AssetPriceStore assetPriceStore,
            LatestAssetPriceReader latestAssetPriceReader,
            DashboardHistoryMetricsReader dashboardHistoryMetricsReader
    ) {
        this.assetPriceStore = assetPriceStore;
        this.latestAssetPriceReader = latestAssetPriceReader;
        this.dashboardHistoryMetricsReader = dashboardHistoryMetricsReader;
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
        Instant updatedAt = assets.stream()
                .map(AssetPrice::collectedAt)
                .max(Instant::compareTo)
                .orElse(historyMetrics.latestCollectedAt());

        return new DashboardResponse(
                resolveStatus(assets, historyMetrics),
                updatedAt,
                assets.size(),
                historyMetrics.historyRowCount(),
                historyMetrics.latestCollectedAt(),
                assets
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
}
