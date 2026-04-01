package com.jean202.assetradar.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public record AssetCompareQuery(
        List<AssetSpec> assets,
        Instant from,
        Instant to,
        BigDecimal baseAmount
) {
    public AssetCompareQuery(
            List<String> rawAssets,
            String fallbackSource,
            String fallbackQuoteCurrency,
            Instant from,
            Instant to,
            BigDecimal baseAmount
    ) {
        this(
                parseAssets(rawAssets, fallbackSource, fallbackQuoteCurrency),
                requireValue(from, "from"),
                requireValue(to, "to"),
                normalizeBaseAmount(baseAmount)
        );
    }

    public AssetCompareQuery {
        assets = List.copyOf(assets);

        if (assets.isEmpty()) {
            throw new IllegalArgumentException("assets is required");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
    }

    private static List<AssetSpec> parseAssets(
            List<String> rawAssets,
            String fallbackSource,
            String fallbackQuoteCurrency
    ) {
        if (rawAssets == null || rawAssets.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<AssetSpec> normalized = new LinkedHashSet<>();
        rawAssets.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> AssetSpec.parse(value, fallbackSource, fallbackQuoteCurrency))
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }

    private static Instant requireValue(Instant value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static BigDecimal normalizeBaseAmount(BigDecimal baseAmount) {
        BigDecimal normalized = baseAmount == null ? new BigDecimal("1000000") : baseAmount;
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("baseAmount must be positive");
        }
        return normalized;
    }

    public record AssetSpec(
            String symbol,
            String source,
            String quoteCurrency
    ) {
        public AssetSpec {
            symbol = normalizeRequired(symbol, "symbol");
            source = normalizeOptional(source);
            quoteCurrency = normalizeOptional(quoteCurrency);
        }

        static AssetSpec parse(String rawValue, String fallbackSource, String fallbackQuoteCurrency) {
            String[] parts = rawValue.split(":");
            if (parts.length == 1) {
                return new AssetSpec(parts[0], fallbackSource, fallbackQuoteCurrency);
            }

            if (parts.length == 3) {
                return new AssetSpec(parts[2], parts[0], parts[1]);
            }

            throw new IllegalArgumentException(
                    "asset must be SYMBOL or SOURCE:QUOTE:SYMBOL"
            );
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
}
