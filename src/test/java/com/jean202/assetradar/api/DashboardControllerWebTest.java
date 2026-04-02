package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.DashboardProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceStore;
import com.jean202.assetradar.query.DashboardHistoryMetrics;
import com.jean202.assetradar.query.DashboardHistoryMetricsReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@WebFluxTest(controllers = DashboardController.class)
@Import(DashboardControllerWebTest.TestConfig.class)
class DashboardControllerWebTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AssetPriceStore assetPriceStore;

    @Autowired
    private RecordingLatestAssetPriceReader latestAssetPriceReader;

    @Autowired
    private RecordingDashboardHistoryMetricsReader historyMetricsReader;

    @BeforeEach
    void setUp() {
        latestAssetPriceReader.prices = List.of();
        historyMetricsReader.metrics = new DashboardHistoryMetrics(0, null);
    }

    @Test
    void returnsGroupedDashboardResponseOverHttp() {
        Instant now = Instant.now();
        latestAssetPriceReader.prices = List.of(
                assetPrice("XAU", "USD", "GOLDAPI", "4674.399902", "0.00000000", now.minusSeconds(20)),
                assetPrice("ETH", "KRW", "UPBIT", "5120000", "-0.00800000", now.minusSeconds(4)),
                assetPrice("BTC", "KRW", "UPBIT", "137500000", "0.02300000", now.minusSeconds(5))
        );
        historyMetricsReader.metrics = new DashboardHistoryMetrics(42, now.minusSeconds(4));

        webTestClient.get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DashboardResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.status()).isEqualTo("live");
                    assertThat(response.assetCount()).isEqualTo(3);
                    assertThat(response.historyRowCount()).isEqualTo(42);
                    assertThat(response.assets())
                            .extracting(price -> "%s:%s".formatted(price.source(), price.symbol()))
                            .containsExactly("GOLDAPI:XAU", "UPBIT:BTC", "UPBIT:ETH");
                    assertThat(response.sourceGroups())
                            .extracting(DashboardSourceGroup::source)
                            .containsExactly("GOLDAPI", "UPBIT");
                    assertThat(response.sourceGroups())
                            .extracting(DashboardSourceGroup::assetTypeLabel)
                            .containsExactly("금", "코인");
                    assertThat(response.sourceGroups())
                            .extracting(DashboardSourceGroup::stale)
                            .containsExactly(false, false);
                    assertThat(response.sourceGroups().get(0).lastUpdatedAgeSeconds())
                            .isBetween(0L, 120L);
                    assertThat(response.sourceGroups().get(1).lastUpdatedAgeSeconds())
                            .isBetween(0L, 30L);
                    assertThat(response.sourceGroups())
                            .extracting(DashboardSourceGroup::lastUpdatedAgo)
                            .allMatch(value -> value.endsWith("ago"));
                    assertThat(response.sourceGroups().get(1).assets())
                            .extracting(AssetPrice::symbol)
                            .containsExactly("BTC", "ETH");
                });
    }

    @Test
    void streamsSnapshotThenLiveUpdateOverSse() {
        AssetPrice snapshotPrice = assetPrice("BTC", "KRW", "UPBIT", "137500000", "0.02300000", Instant.now().minusSeconds(1));
        AssetPrice livePrice = assetPrice("ETH", "KRW", "UPBIT", "5120000", "-0.00800000", Instant.now());
        assetPriceStore.update(snapshotPrice);

        Flux<ServerSentEvent<AssetPrice>> responseBody = webTestClient.get()
                .uri("/api/dashboard/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<AssetPrice>>() {
                })
                .getResponseBody();

        StepVerifier.create(responseBody.take(2))
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo("asset-price");
                    assertThat(event.data()).isNotNull();
                    assertThat(event.data().symbol()).isEqualTo("BTC");
                    assertThat(event.data().source()).isEqualTo("UPBIT");
                })
                .then(() -> assetPriceStore.update(livePrice))
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo("asset-price");
                    assertThat(event.data()).isNotNull();
                    assertThat(event.data().symbol()).isEqualTo("ETH");
                    assertThat(event.data().source()).isEqualTo("UPBIT");
                })
                .verifyComplete();
    }

    private AssetPrice assetPrice(
            String symbol,
            String quoteCurrency,
            String source,
            String price,
            String signedChangeRate,
            Instant collectedAt
    ) {
        return new AssetPrice(
                symbol,
                quoteCurrency,
                source,
                new BigDecimal(price),
                new BigDecimal(signedChangeRate),
                collectedAt
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        AssetPriceStore assetPriceStore() {
            return new AssetPriceStore();
        }

        @Bean
        RecordingLatestAssetPriceReader latestAssetPriceReader() {
            return new RecordingLatestAssetPriceReader();
        }

        @Bean
        RecordingDashboardHistoryMetricsReader dashboardHistoryMetricsReader() {
            return new RecordingDashboardHistoryMetricsReader();
        }

        @Bean
        DashboardProperties dashboardProperties() {
            DashboardProperties properties = new DashboardProperties();
            properties.getStaleThresholds().put("UPBIT", Duration.ofSeconds(30));
            properties.getStaleThresholds().put("GOLDAPI", Duration.ofMinutes(15));
            return properties;
        }
    }

    static class RecordingLatestAssetPriceReader implements LatestAssetPriceReader {
        private List<AssetPrice> prices = List.of();

        @Override
        public Flux<AssetPrice> readLatest(LatestAssetQuery query) {
            return Flux.fromIterable(prices);
        }
    }

    static class RecordingDashboardHistoryMetricsReader implements DashboardHistoryMetricsReader {
        private DashboardHistoryMetrics metrics = new DashboardHistoryMetrics(0, null);

        @Override
        public Mono<DashboardHistoryMetrics> readMetrics() {
            return Mono.just(metrics);
        }
    }
}
