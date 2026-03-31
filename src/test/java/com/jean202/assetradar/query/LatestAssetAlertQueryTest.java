package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAlert;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LatestAssetAlertQueryTest {
    @Test
    void buildsRedisPatternAndMatchesNormalizedValues() {
        LatestAssetAlertQuery query = new LatestAssetAlertQuery(
                "upbit",
                "krw",
                List.of("btc,eth"),
                List.of("critical,warn")
        );

        assertThat(query.redisKeyPattern("alert:latest")).isEqualTo("alert:latest:UPBIT:KRW:*");
        assertThat(query.matches(new AssetAlert(
                "BTC",
                "KRW",
                "UPBIT",
                "PRICE_SURGE",
                "CRITICAL",
                "UP",
                new BigDecimal("100"),
                new BigDecimal("90"),
                new BigDecimal("10"),
                new BigDecimal("0.11111111"),
                new BigDecimal("0.02000000"),
                Instant.parse("2026-03-30T23:59:00Z"),
                60L,
                "message",
                Instant.parse("2026-03-31T00:00:00Z")
        ))).isTrue();
    }
}
