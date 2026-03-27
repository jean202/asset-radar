package com.jean202.assetradar.collector;

import com.jean202.assetradar.domain.AssetPrice;
import reactor.core.publisher.Flux;

public interface AssetCollector {
    Flux<AssetPrice> collect();

    String sourceName();
}
