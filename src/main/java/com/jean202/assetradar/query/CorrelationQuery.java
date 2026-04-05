package com.jean202.assetradar.query;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public record CorrelationQuery(
        List<AssetCompareQuery.AssetSpec> assets,
        Instant from,
        Instant to
) {
    public CorrelationQuery(
            List<String> rawAssets,
            String fallbackSource,
            String fallbackQuoteCurrency,
            Instant from,
            Instant to
    ) {
        this(parseAssets(rawAssets, fallbackSource, fallbackQuoteCurrency), from, to);
    }

    public CorrelationQuery {
        if (assets == null || assets.size() < 2) {
            throw new IllegalArgumentException("at least 2 assets are required");
        }
        assets = List.copyOf(assets);

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

    private static List<AssetCompareQuery.AssetSpec> parseAssets(
            List<String> rawAssets,
            String fallbackSource,
            String fallbackQuoteCurrency
    ) {
        if (rawAssets == null || rawAssets.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<AssetCompareQuery.AssetSpec> normalized = new LinkedHashSet<>();
        rawAssets.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> AssetCompareQuery.AssetSpec.parse(value, fallbackSource, fallbackQuoteCurrency))
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }
}
