package com.jean202.assetradar.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AssetAlert(
        String symbol,
        String quoteCurrency,
        String source,
        String alertType,
        String severity,
        String movement,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal priceChange,
        BigDecimal changeRate,
        BigDecimal thresholdRate,
        Instant baselineAt,
        Long windowSeconds,
        String message,
        Instant alertedAt
) {
}
