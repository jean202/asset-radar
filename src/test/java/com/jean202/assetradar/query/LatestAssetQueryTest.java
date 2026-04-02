package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LatestAssetQueryTest {
    @Test
    void buildsRedisPatternAndMatchesNormalizedValues() {
        LatestAssetQuery query = new LatestAssetQuery("upbit", "krw", List.of("btc,eth"));

        assertThat(query.redisKeyPattern("asset:latest")).isEqualTo("asset:latest:UPBIT:KRW:*");
        assertThat(query.matches(new AssetPrice(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("100"),
                BigDecimal.ZERO,
                Instant.parse("2026-03-31T00:00:00Z")
        ))).isTrue();
    }

    @Test
    void assetHistoryQueryRequiresSymbol() {
        assertThatThrownBy(() -> new AssetHistoryQuery(" ", null, null, null, null, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("symbol is required");
    }
}
