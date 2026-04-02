package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.domain.AssetPrice;
import reactor.core.publisher.Mono;

public interface AssetPriceSink {
    Mono<Void> persist(AssetPrice price);

    String sinkName();
}
