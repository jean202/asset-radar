package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.domain.AssetPrice;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AssetPriceStore {
    private final ConcurrentMap<String, AssetPrice> latestPrices = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastUpdatedAt = new AtomicReference<>();
    private final Sinks.Many<AssetPrice> updates = Sinks.many().multicast().directBestEffort();

    public void update(AssetPrice price) {
        latestPrices.put(keyOf(price), price);
        lastUpdatedAt.set(price.collectedAt());
        updates.tryEmitNext(price);
    }

    public List<AssetPrice> snapshot() {
        return latestPrices.values().stream()
                .sorted(Comparator
                        .comparing(AssetPrice::source)
                        .thenComparing(AssetPrice::quoteCurrency)
                        .thenComparing(AssetPrice::symbol))
                .toList();
    }

    public Instant lastUpdatedAt() {
        return lastUpdatedAt.get();
    }

    public Flux<AssetPrice> stream() {
        return updates.asFlux();
    }

    private String keyOf(AssetPrice price) {
        return "%s:%s:%s".formatted(price.source(), price.quoteCurrency(), price.symbol());
    }
}
