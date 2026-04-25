package com.jean202.assetradar.collector;

import com.jean202.assetradar.config.FinnhubCollectorProperties;
import com.jean202.assetradar.domain.AssetPrice;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class FinnhubCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(FinnhubCollector.class);

    private final FinnhubCollectorProperties properties;
    private final FinnhubPriceDecoder decoder;
    private final WebClient webClient;

    @Autowired
    public FinnhubCollector(
            FinnhubCollectorProperties properties,
            FinnhubPriceDecoder decoder,
            WebClient.Builder webClientBuilder
    ) {
        this(properties, decoder, webClientBuilder.baseUrl(properties.getBaseUrl().toString()).build());
    }

    FinnhubCollector(
            FinnhubCollectorProperties properties,
            FinnhubPriceDecoder decoder,
            WebClient webClient
    ) {
        this.properties = properties;
        this.decoder = decoder;
        this.webClient = webClient;
    }

    @Override
    public Flux<AssetPrice> collect() {
        List<String> symbols = properties.normalizedSymbols();
        if (!properties.isEnabled() || symbols.isEmpty()) {
            log.info("Finnhub collector is disabled or no symbols are configured.");
            return Flux.empty();
        }

        AtomicInteger index = new AtomicInteger(0);

        return Flux.interval(Duration.ZERO, properties.getRefreshInterval())
                .concatMap(ignored -> {
                    int currentIndex = index.getAndUpdate(i -> (i + 1) % symbols.size());
                    String symbol = symbols.get(currentIndex);
                    return fetchPrice(symbol)
                            .onErrorResume(error -> {
                                log.warn("Finnhub collector request failed for {}: {}", symbol, error.getMessage());
                                return Mono.empty();
                            });
                });
    }

    @Override
    public String sourceName() {
        return "finnhub";
    }

    Mono<AssetPrice> fetchPrice(String symbol) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", properties.getApiKey())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(payload -> Mono.justOrEmpty(decoder.decode(payload)))
                .map(quote -> new AssetPrice(
                        symbol,
                        "",
                        "USD",
                        "FINNHUB",
                        quote.price(),
                        quote.signedChangeRate(),
                        quote.collectedAt()
                ));
    }
}
