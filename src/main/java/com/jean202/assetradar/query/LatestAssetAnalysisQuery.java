package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAnalysis;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record LatestAssetAnalysisQuery(
        String source,
        String quoteCurrency,
        Set<String> symbols
) {
    public LatestAssetAnalysisQuery(String source, String quoteCurrency, List<String> rawSymbols) {
        this(source, quoteCurrency, normalizeSymbols(rawSymbols));
    }

    public LatestAssetAnalysisQuery {
        source = normalizeOptional(source);
        quoteCurrency = normalizeOptional(quoteCurrency);
        symbols = symbols == null ? Set.of() : Set.copyOf(symbols);
    }

    public String redisKeyPattern(String redisKeyPrefix) {
        StringBuilder pattern = new StringBuilder(redisKeyPrefix);
        pattern.append(':').append(source == null ? "*" : source);
        pattern.append(':').append(quoteCurrency == null ? "*" : quoteCurrency);
        pattern.append(':').append('*');
        return pattern.toString();
    }

    public boolean matches(AssetAnalysis analysis) {
        if (source != null && !source.equals(analysis.source())) {
            return false;
        }

        if (quoteCurrency != null && !quoteCurrency.equals(analysis.quoteCurrency())) {
            return false;
        }

        return symbols.isEmpty() || symbols.contains(analysis.symbol());
    }

    private static Set<String> normalizeSymbols(List<String> rawSymbols) {
        if (rawSymbols == null || rawSymbols.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        rawSymbols.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(LatestAssetAnalysisQuery::normalizeOptional)
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
