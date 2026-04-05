package com.jean202.assetradar.analysis;

import java.math.BigDecimal;

public record SummaryStatistics(
        long count,
        BigDecimal min,
        BigDecimal max,
        BigDecimal mean,
        BigDecimal stdDev,
        BigDecimal median,
        BigDecimal p5,
        BigDecimal p25,
        BigDecimal p75,
        BigDecimal p95
) {
}
