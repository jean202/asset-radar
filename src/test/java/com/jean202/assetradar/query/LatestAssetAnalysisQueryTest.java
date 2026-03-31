package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jean202.assetradar.domain.AssetAnalysis;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LatestAssetAnalysisQueryTest {
    @Test
    void buildsRedisPatternAndMatchesNormalizedValues() {
        LatestAssetAnalysisQuery query = new LatestAssetAnalysisQuery("upbit", "krw", List.of("btc,eth"));

        assertThat(query.redisKeyPattern("analysis:latest")).isEqualTo("analysis:latest:UPBIT:KRW:*");
        assertThat(query.matches(new AssetAnalysis(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("100"),
                new BigDecimal("90"),
                new BigDecimal("10"),
                new BigDecimal("0.11111111"),
                "UP",
                Instant.parse("2026-03-31T00:00:00Z")
        ))).isTrue();
    }

    @Test
    void analysisHistoryQueryRequiresSymbol() {
        assertThatThrownBy(() -> new AssetAnalysisHistoryQuery(" ", null, null, null, null, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("symbol is required");
    }
}
