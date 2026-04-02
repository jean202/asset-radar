package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;

public record ComparedAssetResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant startedAt,
        Instant endedAt,
        long sampleCount,
        BigDecimal startPrice,
        BigDecimal endPrice,
        BigDecimal priceChange,
        BigDecimal returnRate,
        String movement,
        BigDecimal projectedValue
) {
    public ComparedAssetResponse withoutProjectedValue() {
        return new ComparedAssetResponse(
                symbol,
                source,
                quoteCurrency,
                startedAt,
                endedAt,
                sampleCount,
                startPrice,
                endPrice,
                priceChange,
                returnRate,
                movement,
                null
        );
    }
}
