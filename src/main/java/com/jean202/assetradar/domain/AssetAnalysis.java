package com.jean202.assetradar.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AssetAnalysis(
        String symbol,
        String quoteCurrency,
        String source,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal priceChange,
        BigDecimal changeRate,
        String movement,
        Instant analyzedAt
) {
}
