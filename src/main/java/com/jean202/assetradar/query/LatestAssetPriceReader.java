package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetPrice;
import reactor.core.publisher.Flux;

public interface LatestAssetPriceReader {
    Flux<AssetPrice> readLatest(LatestAssetQuery query);
}
