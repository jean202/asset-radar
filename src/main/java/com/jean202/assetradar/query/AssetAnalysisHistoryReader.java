package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAnalysis;
import reactor.core.publisher.Flux;

public interface AssetAnalysisHistoryReader {
    Flux<AssetAnalysis> readHistory(AssetAnalysisHistoryQuery query);
}
