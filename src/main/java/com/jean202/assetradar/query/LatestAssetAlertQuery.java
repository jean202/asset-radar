package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAlert;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record LatestAssetAlertQuery(
        String source,
        String quoteCurrency,
        Set<String> symbols,
        Set<String> severities
) {
    public LatestAssetAlertQuery(
            String source,
            String quoteCurrency,
            List<String> rawSymbols,
            List<String> rawSeverities
    ) {
        this(source, quoteCurrency, normalizeValues(rawSymbols), normalizeValues(rawSeverities));
    }

    public LatestAssetAlertQuery {
        source = normalizeOptional(source);
        quoteCurrency = normalizeOptional(quoteCurrency);
        symbols = symbols == null ? Set.of() : Set.copyOf(symbols);
        severities = severities == null ? Set.of() : Set.copyOf(severities);
    }

    public String redisKeyPattern(String redisKeyPrefix) {
        StringBuilder pattern = new StringBuilder(redisKeyPrefix);
        pattern.append(':').append(source == null ? "*" : source);
        pattern.append(':').append(quoteCurrency == null ? "*" : quoteCurrency);
        pattern.append(':').append('*');
        return pattern.toString();
    }

    public boolean matches(AssetAlert alert) {
        if (source != null && !source.equals(alert.source())) {
            return false;
        }

        if (quoteCurrency != null && !quoteCurrency.equals(alert.quoteCurrency())) {
            return false;
        }

        if (!symbols.isEmpty() && !symbols.contains(alert.symbol())) {
            return false;
        }

        return severities.isEmpty() || severities.contains(alert.severity());
    }

    private static Set<String> normalizeValues(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        rawValues.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(LatestAssetAlertQuery::normalizeOptional)
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
