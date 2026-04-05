package com.jean202.assetradar.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MovingAverageResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        String type,
        int window,
        int dataPoints,
        List<MovingAveragePoint> points
) {
    public record MovingAveragePoint(
            Instant timestamp,
            BigDecimal price,
            BigDecimal movingAverage
    ) {
    }
}
