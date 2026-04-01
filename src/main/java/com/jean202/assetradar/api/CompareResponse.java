package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompareResponse(
        Instant from,
        Instant to,
        BigDecimal baseAmount,
        int requestedCount,
        int count,
        boolean projectedValueComparable,
        List<String> quoteCurrencies,
        String projectedValueWarning,
        List<ComparedAssetResponse> comparisons
) {
}
