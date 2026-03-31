package com.jean202.assetradar.analysis;

import com.jean202.assetradar.domain.AssetAnalysis;
import reactor.core.publisher.Mono;

public interface AssetAnalysisSink {
    Mono<Void> persist(AssetAnalysis analysis);

    String sinkName();
}
