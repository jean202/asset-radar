package com.jean202.assetradar.collector;

import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import reactor.core.publisher.Flux;

public class CoinCollector implements AssetCollector {
    @Override
    public Flux<AssetPrice> collect() {
        return Flux.just(new AssetPrice("BTC", "UPBIT", BigDecimal.ZERO, Instant.now()));
    }

    @Override
    public String sourceName() {
        return "coin";
    }
}
