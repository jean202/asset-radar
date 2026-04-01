package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.AssetComparator;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = AssetCompareController.class)
@Import({ApiExceptionHandler.class, AssetCompareControllerErrorTest.MockConfig.class})
class AssetCompareControllerErrorTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AssetCompareWindowReader assetCompareWindowReader;

    @Autowired
    private AssetComparator assetComparator;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(assetCompareWindowReader, assetComparator);
    }

    @Test
    void returnsBadRequestJsonForInvalidPeriod() {
        webTestClient.get()
                .uri("/api/compare?assets=BTC&period=bad")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("period must be one of Ns, Nm, Nh, Nd, Nw")
                .jsonPath("$.path").isEqualTo("/api/compare");
    }

    @Test
    void returnsBadRequestJsonForInvalidAssetFormat() {
        webTestClient.get()
                .uri("/api/compare?assets=UPBIT:BTC&period=30d")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("asset must be SYMBOL or SOURCE:QUOTE:SYMBOL")
                .jsonPath("$.path").isEqualTo("/api/compare");
    }

    @Test
    void returnsBadRequestJsonForNegativeBaseAmount() {
        webTestClient.get()
                .uri("/api/compare?assets=BTC&period=30d&baseAmount=-1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("baseAmount must be positive")
                .jsonPath("$.path").isEqualTo("/api/compare");
    }

    @Test
    void returnsBadRequestJsonForMalformedBaseAmount() {
        webTestClient.get()
                .uri("/api/compare?assets=BTC&period=30d&baseAmount=abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("baseAmount must be a valid decimal number")
                .jsonPath("$.path").isEqualTo("/api/compare");
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        AssetCompareWindowReader assetCompareWindowReader() {
            return Mockito.mock(AssetCompareWindowReader.class);
        }

        @Bean
        AssetComparator assetComparator() {
            return Mockito.mock(AssetComparator.class);
        }
    }
}
