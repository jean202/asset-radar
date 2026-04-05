package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;

public record SummaryResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        long dataPoints,
        BigDecimal min,
        BigDecimal max,
        BigDecimal mean,
        BigDecimal stdDev,
        BigDecimal median,
        BigDecimal p5,
        BigDecimal p25,
        BigDecimal p75,
        BigDecimal p95,
        BigDecimal latestPrice,
        BigDecimal returnRate,
        BigDecimal maxDrawdown
) {
}
