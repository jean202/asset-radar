package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetPrice;
import reactor.core.publisher.Flux;

public interface AssetPriceHistoryReader {
    Flux<AssetPrice> readHistory(AssetHistoryQuery query);
}
