package com.jean202.assetradar.collector;

import com.jean202.assetradar.config.CoinCollectorProperties;
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
public class CoinCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(CoinCollector.class);

    private final CoinCollectorProperties properties;
    private final UpbitTickerDecoder decoder;
    private final WebSocketClient webSocketClient;

    public CoinCollector(CoinCollectorProperties properties, UpbitTickerDecoder decoder) {
        this.properties = properties;
        this.decoder = decoder;
        this.webSocketClient = new ReactorNettyWebSocketClient();
    }

    @Override
    public Flux<AssetPrice> collect() {
        if (!properties.isEnabled() || properties.normalizedSymbols().isEmpty()) {
            log.info("Coin collector is disabled or no symbols are configured.");
            return Flux.empty();
        }

        return Flux.defer(this::connectToUpbit)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .jitter(0.2)
                        .doBeforeRetry(signal -> log.warn(
                                "Retrying coin collector after error: {}",
                                signal.failure().getMessage()
                        )));
    }

    @Override
    public String sourceName() {
        return "upbit";
    }

    private Flux<AssetPrice> connectToUpbit() {
        var subscriptionPayload = decoder.createSubscriptionPayload(properties.normalizedSymbols());

        log.info("Connecting to Upbit websocket for symbols {}", properties.normalizedSymbols());

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
                .concatWith(Flux.error(new IllegalStateException("Upbit websocket connection closed")));
    }
}
