package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceStore;
import com.jean202.assetradar.query.DashboardHistoryMetrics;
import com.jean202.assetradar.query.DashboardHistoryMetricsReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DashboardControllerTest {
    private AssetPriceStore assetPriceStore;
    private DashboardController controller;
    private RecordingLatestReader latestReader;
    private RecordingHistoryMetricsReader historyMetricsReader;

    @BeforeEach
    void setUp() {
        assetPriceStore = new AssetPriceStore();
        latestReader = new RecordingLatestReader();
        historyMetricsReader = new RecordingHistoryMetricsReader();
        controller = new DashboardController(assetPriceStore, latestReader, historyMetricsReader);
    }

    @Test
    void returnsLiveSnapshotWhenPriceExists() {
        AssetPrice price = new AssetPrice(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000"),
                new BigDecimal("0.023"),
                Instant.parse("2026-03-30T08:15:30Z")
        );
        latestReader.prices = List.of(price);
        historyMetricsReader.metrics = new DashboardHistoryMetrics(42, Instant.parse("2026-03-30T08:15:30Z"));

        DashboardResponse response = controller.dashboard().block();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("live");
        assertThat(response.assetCount()).isEqualTo(1);
        assertThat(response.historyRowCount()).isEqualTo(42);
        assertThat(response.assets()).extracting(AssetPrice::symbol).containsExactly("BTC");
    }

    @Test
    void returnsDegradedWhenOnlyHistoryExists() {
        historyMetricsReader.metrics = new DashboardHistoryMetrics(10, Instant.parse("2026-03-30T08:00:00Z"));

        DashboardResponse response = controller.dashboard().block();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("degraded");
        assertThat(response.assetCount()).isEqualTo(0);
        assertThat(response.historyUpdatedAt()).isEqualTo(Instant.parse("2026-03-30T08:00:00Z"));
    }

    @Test
    void streamIncludesSnapshotThenLiveUpdates() {
        assetPriceStore.update(new AssetPrice(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000"),
                new BigDecimal("0.023"),
                Instant.parse("2026-03-30T08:15:30Z")
        ));

        StepVerifier.create(controller.stream().map(ServerSentEvent::data).take(2))
                .assertNext(price -> assertThat(price.symbol()).isEqualTo("BTC"))
                .then(() -> assetPriceStore.update(new AssetPrice(
                        "ETH",
                        "KRW",
                        "UPBIT",
                        new BigDecimal("5120000"),
                        new BigDecimal("-0.008"),
                        Instant.parse("2026-03-30T08:15:35Z")
                )))
                .assertNext(price -> assertThat(price.symbol()).isEqualTo("ETH"))
                .verifyComplete();
    }

    private static final class RecordingLatestReader implements LatestAssetPriceReader {
        private List<AssetPrice> prices = List.of();

        @Override
        public Flux<AssetPrice> readLatest(LatestAssetQuery query) {
            return Flux.fromIterable(prices);
        }
    }

    private static final class RecordingHistoryMetricsReader implements DashboardHistoryMetricsReader {
        private DashboardHistoryMetrics metrics = new DashboardHistoryMetrics(0, null);

        @Override
        public Mono<DashboardHistoryMetrics> readMetrics() {
            return Mono.just(metrics);
        }
    }
}
