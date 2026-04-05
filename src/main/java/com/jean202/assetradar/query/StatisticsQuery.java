package com.jean202.assetradar.query;

import java.time.Instant;
import java.util.Locale;

public record StatisticsQuery(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to
) {
    public StatisticsQuery {
        symbol = normalizeRequired(symbol, "symbol");
        source = normalizeOptional(source);
        quoteCurrency = normalizeOptional(quoteCurrency);

        if (from == null) {
            throw new IllegalArgumentException("from is required");
        }
        if (to == null) {
            throw new IllegalArgumentException("to is required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
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
