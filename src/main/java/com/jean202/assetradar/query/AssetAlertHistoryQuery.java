package com.jean202.assetradar.query;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record AssetAlertHistoryQuery(
        String symbol,
        String source,
        String quoteCurrency,
        Set<String> severities,
        Instant from,
        Instant to,
        int limit
) {
    public AssetAlertHistoryQuery(
            String symbol,
            String source,
            String quoteCurrency,
            List<String> rawSeverities,
            Instant from,
            Instant to,
            int limit
    ) {
        this(symbol, source, quoteCurrency, normalizeValues(rawSeverities), from, to, limit);
    }

    public AssetAlertHistoryQuery {
        symbol = normalizeOptional(symbol);
        source = normalizeOptional(source);
        quoteCurrency = normalizeOptional(quoteCurrency);
        severities = severities == null ? Set.of() : Set.copyOf(severities);
    }

    private static Set<String> normalizeValues(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        rawValues.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(AssetAlertHistoryQuery::normalizeOptional)
                .filter(java.util.Objects::nonNull)
                .forEach(normalized::add);
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
