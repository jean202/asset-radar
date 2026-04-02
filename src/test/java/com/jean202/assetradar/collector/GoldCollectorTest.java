package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.GoldCollectorProperties;
import com.jean202.assetradar.domain.AssetPrice;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GoldCollectorTest {
    @Test
    void emitsSpotPriceWithComputedChangeRate() {
        GoldCollectorProperties properties = new GoldCollectorProperties();
        properties.setRefreshInterval(Duration.ofMillis(10));

        ExchangeFunction exchangeFunction = new SequencedExchangeFunction(
                """
                        {
                          "currency": "USD",
                          "price": 4674.399902,
                          "symbol": "XAU",
                          "updatedAt": "2026-04-01T04:28:11Z"
                        }
                        """,
                """
                        {
                          "currency": "USD",
                          "price": 4700.000000,
                          "symbol": "XAU",
                          "updatedAt": "2026-04-01T04:33:11Z"
                        }
                        """
        );

        GoldCollector collector = new GoldCollector(
                properties,
                new GoldApiPriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(exchangeFunction).build()
        );

        StepVerifier.create(collector.collect().take(2))
                .assertNext(price -> {
                    assertThat(price.source()).isEqualTo("GOLDAPI");
                    assertThat(price.symbol()).isEqualTo("XAU");
                    assertThat(price.quoteCurrency()).isEqualTo("USD");
                    assertThat(price.price()).isEqualByComparingTo("4674.399902");
                    assertThat(price.signedChangeRate()).isZero();
                })
                .assertNext(price -> {
                    assertThat(price.price()).isEqualByComparingTo("4700.000000");
                    assertThat(price.signedChangeRate()).isEqualByComparingTo("0.00547666");
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenDisabled() {
        GoldCollectorProperties properties = new GoldCollectorProperties();
        properties.setEnabled(false);

        GoldCollector collector = new GoldCollector(
                properties,
                new GoldApiPriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(request -> Mono.error(new AssertionError("should not call"))).build()
        );

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }

    private static final class SequencedExchangeFunction implements ExchangeFunction {
        private final String[] payloads;
        private int index;

        private SequencedExchangeFunction(String... payloads) {
            this.payloads = payloads;
        }

        @Override
        public Mono<ClientResponse> exchange(org.springframework.web.reactive.function.client.ClientRequest request) {
            int currentIndex = Math.min(index, payloads.length - 1);
            String payload = payloads[currentIndex];
            index++;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .build());
        }
    }
}
