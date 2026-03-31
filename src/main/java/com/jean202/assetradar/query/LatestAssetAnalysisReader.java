package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAnalysis;
import reactor.core.publisher.Flux;

public interface LatestAssetAnalysisReader {
    Flux<AssetAnalysis> readLatest(LatestAssetAnalysisQuery query);
}
