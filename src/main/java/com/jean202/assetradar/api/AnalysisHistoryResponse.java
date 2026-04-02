package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAnalysis;
import java.time.Instant;
import java.util.List;

public record AnalysisHistoryResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        int count,
        List<AssetAnalysis> analyses
) {
}
