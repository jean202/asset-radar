package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssetCompareQueryTest {
    @Test
    void parsesSimpleAndExplicitAssetSpecs() {
        AssetCompareQuery query = new AssetCompareQuery(
                List.of("btc, goldapi:usd:xau"),
                "upbit",
                "krw",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                new BigDecimal("1000000")
        );

        assertThat(query.assets()).containsExactly(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD")
        );
    }

    @Test
    void rejectsInvalidAssetSpecFormat() {
        assertThatThrownBy(() -> new AssetCompareQuery(
                List.of("UPBIT:BTC"),
                null,
                null,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                new BigDecimal("1000000")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("asset must be SYMBOL or SOURCE:QUOTE:SYMBOL");
    }

    @Test
    void requiresAtLeastOneAsset() {
        assertThatThrownBy(() -> new AssetCompareQuery(
                List.of(),
                null,
                null,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                new BigDecimal("1000000")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("assets is required");
    }
}
