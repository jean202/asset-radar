package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CorrelationResponse(
        Instant from,
        Instant to,
        int assetCount,
        List<CorrelationPair> pairs
) {
    public record CorrelationPair(
            String symbolA,
            String sourceA,
            String symbolB,
            String sourceB,
            BigDecimal correlation,
            long overlappingSamples
    ) {
    }
}
