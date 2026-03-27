package com.jean202.assetradar.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AssetPrice(String symbol, String market, BigDecimal price, Instant collectedAt) {
}
