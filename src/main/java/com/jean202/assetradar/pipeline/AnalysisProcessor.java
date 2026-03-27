package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.domain.AssetPrice;
import reactor.core.publisher.Flux;

public class AnalysisProcessor {
    public Flux<AssetPrice> passThrough(Flux<AssetPrice> source) {
        return source;
    }
}
