package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.StockKrCollectorProperties;
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

class StockKrCollectorTest {
    @Test
    void emitsStockPricesRoundRobin() {
        StockKrCollectorProperties properties = new StockKrCollectorProperties();
        properties.setRefreshInterval(Duration.ofMillis(10));
        properties.setSymbols(List.of("005930", "000660"));

        ExchangeFunction exchangeFunction = request -> {
            String uri = request.url().toString();
            String payload;
            if (uri.contains("005930")) {
                payload = """
                        {
                          "output": {
                            "stck_shrn_iscd": "005930",
                            "hts_kor_isnm": "삼성전자",
                            "stck_prpr": "78500",
                            "prdy_ctrt": "1.20"
                          }
                        }
                        """;
            } else {
                payload = """
                        {
                          "output": {
                            "stck_shrn_iscd": "000660",
                            "hts_kor_isnm": "SK하이닉스",
                            "stck_prpr": "215000",
                            "prdy_ctrt": "3.10"
                          }
                        }
                        """;
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(payload)
                    .build());
        };

        StockKrCollector collector = new StockKrCollector(
                properties,
                new KisPriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(exchangeFunction).build()
        );

        StepVerifier.create(collector.collect().take(2))
                .assertNext(price -> {
                    assertThat(price.source()).isEqualTo("KIS");
                    assertThat(price.symbol()).isEqualTo("005930");
                    assertThat(price.quoteCurrency()).isEqualTo("KRW");
                    assertThat(price.price()).isEqualByComparingTo("78500");
                    assertThat(price.signedChangeRate()).isEqualByComparingTo("0.0120");
                })
                .assertNext(price -> {
                    assertThat(price.symbol()).isEqualTo("000660");
                    assertThat(price.price()).isEqualByComparingTo("215000");
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenDisabled() {
        StockKrCollectorProperties properties = new StockKrCollectorProperties();
        properties.setEnabled(false);

        StockKrCollector collector = new StockKrCollector(
                properties,
                new KisPriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(request -> Mono.error(new AssertionError("should not call"))).build()
        );

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenNoSymbols() {
        StockKrCollectorProperties properties = new StockKrCollectorProperties();
        properties.setSymbols(List.of());

        StockKrCollector collector = new StockKrCollector(
                properties,
                new KisPriceDecoder(new ObjectMapper()),
                WebClient.builder().exchangeFunction(request -> Mono.error(new AssertionError("should not call"))).build()
        );

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }
}
