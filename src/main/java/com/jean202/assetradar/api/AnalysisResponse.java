package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAnalysis;
import java.util.List;

public record AnalysisResponse(
        String source,
        String quoteCurrency,
        int count,
        List<AssetAnalysis> analyses
) {
}
