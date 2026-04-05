package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorrelationQueryTest {
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2025-01-02T00:00:00Z");

    @Test
    void validQuery() {
        List<AssetCompareQuery.AssetSpec> assets = List.of(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD")
        );
        CorrelationQuery q = new CorrelationQuery(assets, NOW, LATER);
        assertThat(q.assets()).hasSize(2);
    }

    @Test
    void requiresAtLeastTwoAssets() {
        List<AssetCompareQuery.AssetSpec> single = List.of(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW")
        );
        assertThatThrownBy(() -> new CorrelationQuery(single, NOW, LATER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 assets");
    }

    @Test
    void nullAssetsThrows() {
        assertThatThrownBy(() -> new CorrelationQuery(null, NOW, LATER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromMustBeBeforeTo() {
        List<AssetCompareQuery.AssetSpec> assets = List.of(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD")
        );
        assertThatThrownBy(() -> new CorrelationQuery(assets, LATER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }
}
