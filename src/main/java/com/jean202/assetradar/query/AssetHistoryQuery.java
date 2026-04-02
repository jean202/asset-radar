package com.jean202.assetradar.query;

import java.time.Instant;
import java.util.Locale;

public record AssetHistoryQuery(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        int limit
) {
    public AssetHistoryQuery {
        symbol = normalizeRequired(symbol, "symbol");
        source = normalizeOptional(source);
        quoteCurrency = normalizeOptional(quoteCurrency);
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}
