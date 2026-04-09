package com.jean202.assetradar.collector;

import com.jean202.assetradar.config.BinanceCollectorProperties;
import com.jean202.assetradar.domain.AssetPrice;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
public class BinanceCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(BinanceCollector.class);

    private final BinanceCollectorProperties properties;
    private final BinanceTickerDecoder decoder;
    private final WebSocketClient webSocketClient;

    public BinanceCollector(BinanceCollectorProperties properties, BinanceTickerDecoder decoder) {
        this.properties = properties;
        this.decoder = decoder;
        this.webSocketClient = new ReactorNettyWebSocketClient();
    }

    @Override
    public Flux<AssetPrice> collect() {
        if (!properties.isEnabled() || properties.normalizedSymbols().isEmpty()) {
            log.info("Binance collector is disabled or no symbols are configured.");
            return Flux.empty();
        }

        return Flux.defer(this::connectToBinance)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .jitter(0.2)
                        .doBeforeRetry(signal -> log.warn(
                                "Retrying Binance collector after error: {}",
                                signal.failure().getMessage()
                        )));
    }

    @Override
    public String sourceName() {
        return "binance";
    }

    private Flux<AssetPrice> connectToBinance() {
        var subscriptionPayload = decoder.createSubscriptionPayload(properties.normalizedSymbols());

        log.info("Connecting to Binance websocket for symbols {}", properties.normalizedSymbols());

        return Flux.<AssetPrice>create(sink -> {
                    Disposable disposable = webSocketClient.execute(properties.getWebsocketUrl(), session ->
                                    session.send(Flux.just(session.textMessage(subscriptionPayload)))
                                            .thenMany(session.receive()
                                                    .map(message -> message.getPayloadAsText())
                                                    .map(decoder::decode)
                                                    .filter(Objects::nonNull)
                                                    .doOnNext(sink::next))
                                            .then())
                            .subscribe(unused -> {
                            }, sink::error, sink::complete);

                    sink.onDispose(disposable);
                })
                .concatWith(Flux.error(new IllegalStateException("Binance websocket connection closed")));
    }
}
