package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(controllers = AssetRecommendationController.class)
@Import(AssetRecommendationControllerWebTest.TestConfig.class)
class AssetRecommendationControllerWebTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returnsRecommendationForSymbolPathVariable() {
        webTestClient.get()
                .uri("/api/recommendations/symbol/BTC?source=upbit&quoteCurrency=krw")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.symbol").isEqualTo("BTC")
                .jsonPath("$.action").exists()
                .jsonPath("$.confidence").exists();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        LatestAssetAnalysisReader latestAssetAnalysisReader() {
            return query -> Flux.just(analysis(symbolFrom(query)));
        }

        @Bean
        RecommendationEngine recommendationEngine() {
            return new RecommendationEngine();
        }

        private static String symbolFrom(LatestAssetAnalysisQuery query) {
            return query.symbols().stream().findFirst().orElse("BTC");
        }

        private static AssetAnalysis analysis(String symbol) {
            return new AssetAnalysis(
                    symbol,
                    "KRW",
                    "UPBIT",
                    new BigDecimal("137500000"),
                    new BigDecimal("136900000"),
                    new BigDecimal("600000"),
                    new BigDecimal("0.00438276"),
                    "UP",
                    Instant.parse("2026-05-17T10:15:03Z")
            );
        }
    }
}
