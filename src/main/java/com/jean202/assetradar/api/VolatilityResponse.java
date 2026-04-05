package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VolatilityResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        int window,
        int dataPoints,
        BigDecimal currentVolatility,
        BigDecimal annualizedVolatility,
        List<VolatilityPoint> points
) {
    public record VolatilityPoint(
            Instant timestamp,
            BigDecimal volatility
    ) {
    }
}
