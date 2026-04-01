package com.jean202.assetradar.collector;

import com.jean202.assetradar.collector.GoldApiPriceDecoder.GoldApiSpotPrice;
import com.jean202.assetradar.config.GoldCollectorProperties;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GoldCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(GoldCollector.class);

    private final GoldCollectorProperties properties;
    private final GoldApiPriceDecoder decoder;
    private final WebClient webClient;
    private final AtomicReference<BigDecimal> previousPrice = new AtomicReference<>();

    @Autowired
    public GoldCollector(
            GoldCollectorProperties properties,
            GoldApiPriceDecoder decoder,
            WebClient.Builder webClientBuilder
    ) {
        this(properties, decoder, webClientBuilder.baseUrl(properties.getBaseUrl().toString()).build());
    }

    GoldCollector(
            GoldCollectorProperties properties,
            GoldApiPriceDecoder decoder,
            WebClient webClient
    ) {
        this.properties = properties;
        this.decoder = decoder;
        this.webClient = webClient;
    }

    @Override
    public Flux<AssetPrice> collect() {
        if (!properties.isEnabled()) {
            log.info("Gold collector is disabled.");
            return Flux.empty();
        }

        String symbol = properties.normalizedSymbol();
        if (symbol.isBlank()) {
            log.info("Gold collector is enabled but no symbol is configured.");
            return Flux.empty();
        }

        return Flux.interval(java.time.Duration.ZERO, properties.getRefreshInterval())
                .concatMap(ignored -> fetchLatestPrice(symbol)
                        .onErrorResume(error -> {
                            log.warn("Gold collector request failed: {}", error.getMessage());
                            return Mono.empty();
                        }));
    }

    @Override
    public String sourceName() {
        return properties.normalizedSource().toLowerCase(java.util.Locale.ROOT);
    }

    Mono<AssetPrice> fetchLatestPrice(String symbol) {
        return webClient.get()
                .uri("/price/{symbol}", symbol)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(payload -> Mono.justOrEmpty(decoder.decode(payload)))
                .map(spotPrice -> toAssetPrice(spotPrice, properties.normalizedSource()));
    }

    private AssetPrice toAssetPrice(GoldApiSpotPrice spotPrice, String source) {
        BigDecimal signedChangeRate = calculateSignedChangeRate(spotPrice.price());

        return new AssetPrice(
                spotPrice.symbol(),
                spotPrice.quoteCurrency(),
                source,
                spotPrice.price(),
                signedChangeRate,
                spotPrice.updatedAt()
        );
    }

    private BigDecimal calculateSignedChangeRate(BigDecimal currentPrice) {
        BigDecimal previous = previousPrice.getAndSet(currentPrice);
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(previous)
                .divide(previous, 8, RoundingMode.HALF_UP);
    }
}
