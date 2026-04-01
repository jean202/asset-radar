package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.AssetComparator;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindow;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AssetCompareController.class)
@Import(AssetCompareControllerWebTest.TestConfig.class)
class AssetCompareControllerWebTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RecordingAssetCompareWindowReader compareWindowReader;

    @BeforeEach
    void setUp() {
        compareWindowReader.windows.clear();
    }

    @Test
    void returnsSortedComparisonResponseOverHttp() {
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                window("BTC", "KRW", "UPBIT", "100", "120")
        );
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD"),
                window("XAU", "USD", "GOLDAPI", "200", "210")
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/compare")
                        .queryParam("assets", "UPBIT:KRW:BTC,GOLDAPI:USD:XAU")
                        .queryParam("from", "2026-03-01T00:00:00Z")
                        .queryParam("to", "2026-03-31T00:00:00Z")
                        .queryParam("baseAmount", "1000000")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CompareResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.requestedCount()).isEqualTo(2);
                    assertThat(response.count()).isEqualTo(2);
                    assertThat(response.projectedValueComparable()).isFalse();
                    assertThat(response.quoteCurrencies()).containsExactly("KRW", "USD");
                    assertThat(response.projectedValueWarning())
                            .isEqualTo("projectedValue is only comparable when all assets share the same quoteCurrency");
                    assertThat(response.comparisons())
                            .extracting(comparedAsset -> "%s:%s".formatted(comparedAsset.source(), comparedAsset.symbol()))
                            .containsExactly("UPBIT:BTC", "GOLDAPI:XAU");
                    assertThat(response.comparisons())
                            .extracting(ComparedAssetResponse::returnRate)
                            .containsExactly(new BigDecimal("0.20000000"), new BigDecimal("0.05000000"));
                    assertThat(response.comparisons())
                            .extracting(ComparedAssetResponse::projectedValue)
                            .containsOnlyNulls();
                });
    }

    private AssetCompareWindow window(
            String symbol,
            String quoteCurrency,
            String source,
            String startPrice,
            String endPrice
    ) {
        return new AssetCompareWindow(
                new AssetCompareQuery.AssetSpec(symbol, source, quoteCurrency),
                new AssetPrice(
                        symbol,
                        quoteCurrency,
                        source,
                        new BigDecimal(startPrice),
                        BigDecimal.ZERO,
                        Instant.parse("2026-03-01T00:00:00Z")
                ),
                new AssetPrice(
                        symbol,
                        quoteCurrency,
                        source,
                        new BigDecimal(endPrice),
                        BigDecimal.ZERO,
                        Instant.parse("2026-03-31T00:00:00Z")
                ),
                2
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        RecordingAssetCompareWindowReader assetCompareWindowReader() {
            return new RecordingAssetCompareWindowReader();
        }

        @Bean
        AssetComparator assetComparator() {
            return new AssetComparator();
        }
    }

    static class RecordingAssetCompareWindowReader implements AssetCompareWindowReader {
        private final Map<AssetCompareQuery.AssetSpec, AssetCompareWindow> windows = new HashMap<>();

        @Override
        public Mono<AssetCompareWindow> readWindow(AssetCompareQuery.AssetSpec asset, Instant from, Instant to) {
            return Mono.justOrEmpty(windows.get(asset));
        }
    }
}
