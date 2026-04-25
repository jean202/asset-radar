package com.jean202.assetradar.collector;

import com.jean202.assetradar.config.StockKrCollectorProperties;
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
public class StockKrCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(StockKrCollector.class);

    private final StockKrCollectorProperties properties;
    private final KisPriceDecoder decoder;
    private final KisTokenManager tokenManager;
    private final WebClient webClient;

    @Autowired
    public StockKrCollector(
            StockKrCollectorProperties properties,
            KisPriceDecoder decoder,
            KisTokenManager tokenManager,
            WebClient.Builder webClientBuilder
    ) {
        this(properties, decoder, tokenManager, webClientBuilder.baseUrl(properties.getBaseUrl().toString()).build());
    }

    StockKrCollector(
            StockKrCollectorProperties properties,
            KisPriceDecoder decoder,
            KisTokenManager tokenManager,
            WebClient webClient
    ) {
        this.properties = properties;
        this.decoder = decoder;
        this.tokenManager = tokenManager;
        this.webClient = webClient;
    }

    @Override
    public Flux<AssetPrice> collect() {
        List<String> symbols = properties.normalizedSymbols();
        if (!properties.isEnabled() || symbols.isEmpty()) {
            log.info("Stock KR collector is disabled or no symbols are configured.");
            return Flux.empty();
        }

        AtomicInteger index = new AtomicInteger(0);

        return Flux.interval(Duration.ZERO, properties.getRefreshInterval())
                .concatMap(ignored -> {
                    int currentIndex = index.getAndUpdate(i -> (i + 1) % symbols.size());
                    String symbol = symbols.get(currentIndex);
                    return fetchPrice(symbol)
                            .onErrorResume(error -> {
                                log.warn("Stock KR collector request failed for {}: {}", symbol, error.getMessage());
                                return Mono.empty();
                            });
                });
    }

    @Override
    public String sourceName() {
        return properties.normalizedSource().toLowerCase(java.util.Locale.ROOT);
    }

    Mono<AssetPrice> fetchPrice(String symbol) {
        return tokenManager.getToken()
                .switchIfEmpty(Mono.error(new IllegalStateException("KIS access token unavailable — check ASSET_RADAR_KIS_APP_KEY / ASSET_RADAR_KIS_APP_SECRET")))
                .flatMap(token -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                .queryParam("FID_INPUT_ISCD", symbol)
                                .build())
                        .header("Authorization", "Bearer " + token)
                        .header("tr_id", "FHKST01010100")
                        .header("appkey", properties.getAppKey())
                        .header("appsecret", properties.getAppSecret())
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(payload -> Mono.justOrEmpty(decoder.decode(payload)))
                        .map(stockPrice -> toAssetPrice(stockPrice, properties.normalizedSource())));
    }

    private AssetPrice toAssetPrice(KisPriceDecoder.KisStockPrice stockPrice, String source) {
        return new AssetPrice(
                stockPrice.symbol(),
                properties.nameFor(stockPrice.symbol()),
                "KRW",
                source,
                stockPrice.price(),
                stockPrice.signedChangeRate(),
                stockPrice.collectedAt()
        );
    }
}
