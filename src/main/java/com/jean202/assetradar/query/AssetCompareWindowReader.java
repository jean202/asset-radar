package com.jean202.assetradar.query;

import reactor.core.publisher.Mono;

public interface AssetCompareWindowReader {
    Mono<AssetCompareWindow> readWindow(AssetCompareQuery.AssetSpec asset, java.time.Instant from, java.time.Instant to);
}
