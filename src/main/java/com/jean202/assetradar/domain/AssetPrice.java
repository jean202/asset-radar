package com.jean202.assetradar.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AssetPrice(
        String symbol,
        String quoteCurrency,
        String source,
        BigDecimal price,
        BigDecimal signedChangeRate,
        Instant collectedAt
) {
}
