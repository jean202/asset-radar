package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.StockUsCollectorProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class StockUsCollectorTest {
    @Test
    void emitsQuotePricesRoundRobin() {
        StockUsCollectorProperties properties = new StockUsCollectorProperties();
        properties.setRefreshInterval(Duration.ofMillis(10));
        properties.setSymbols(List.of("AAPL", "NVDA"));

        ExchangeFunction exchangeFunction = request -> {
            String uri = request.url().toString();
            String payload;
            if (uri.contains("AAPL")) {
                payload = """
                        {
                          "Global Quote": {
                            "01. symbol": "AAPL",
                            "05. price": "198.5000",
                            "10. change percent": "-0.5013%"
                          }
                        }
                        """;
            } else {
                payload = """
                        {
                          "Global Quote": {
                            "01. symbol": "NVDA",
                            "05. price": "890.2000",
                            "10. change percent": "4.2500%"
                          }
                        }
                        """;
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .build());
        };

        StockUsCollector collector = new StockUsCollector(
                properties,
                new AlphaVantagePriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(exchangeFunction).build()
        );

        StepVerifier.create(collector.collect().take(2))
                .assertNext(price -> {
                    assertThat(price.source()).isEqualTo("ALPHAVANTAGE");
                    assertThat(price.symbol()).isEqualTo("AAPL");
                    assertThat(price.quoteCurrency()).isEqualTo("USD");
                    assertThat(price.price()).isEqualByComparingTo("198.5000");
                    assertThat(price.signedChangeRate()).isEqualByComparingTo("-0.005013");
                })
                .assertNext(price -> {
                    assertThat(price.symbol()).isEqualTo("NVDA");
                    assertThat(price.price()).isEqualByComparingTo("890.2000");
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenDisabled() {
        StockUsCollectorProperties properties = new StockUsCollectorProperties();
        properties.setEnabled(false);

        StockUsCollector collector = new StockUsCollector(
                properties,
                new AlphaVantagePriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(request -> Mono.error(new AssertionError("should not call"))).build()
        );

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenNoSymbols() {
        StockUsCollectorProperties properties = new StockUsCollectorProperties();
        properties.setSymbols(List.of());

        StockUsCollector collector = new StockUsCollector(
                properties,
                new AlphaVantagePriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(request -> Mono.error(new AssertionError("should not call"))).build()
        );

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }
}
