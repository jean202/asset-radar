package com.jean202.assetradar.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.AlertProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.domain.AssetAnalysis;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssetAlertRuleEngineTest {
    @Test
    void emitsSeverityBasedOnWindowChangeRate() {
        AssetAlertRuleEngine ruleEngine = new AssetAlertRuleEngine(new AlertProperties());

        AssetAlert infoAlert = ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:00:00Z"),
                analysis("BTC", "100.6", "2026-03-31T00:01:00Z")
        ).orElseThrow();
        AssetAlert warnAlert = ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:00:00Z"),
                analysis("BTC", "101.5", "2026-03-31T00:01:00Z")
        ).orElseThrow();
        AssetAlert criticalAlert = ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:00:00Z"),
                analysis("BTC", "97", "2026-03-31T00:01:00Z")
        ).orElseThrow();

        assertThat(infoAlert.severity()).isEqualTo("INFO");
        assertThat(warnAlert.severity()).isEqualTo("WARN");
        assertThat(criticalAlert.severity()).isEqualTo("CRITICAL");
        assertThat(criticalAlert.alertType()).isEqualTo("PRICE_DROP");
        assertThat(warnAlert.baselineAt()).isEqualTo(Instant.parse("2026-03-31T00:00:00Z"));
        assertThat(warnAlert.windowSeconds()).isEqualTo(60L);
    }

    @Test
    void skipsSubThresholdFlatAndInvalidWindowComparisons() {
        AssetAlertRuleEngine ruleEngine = new AssetAlertRuleEngine(new AlertProperties());

        assertThat(ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:00:00Z"),
                analysis("BTC", "100.4", "2026-03-31T00:01:00Z")
        )).isEmpty();
        assertThat(ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:00:00Z"),
                analysis("BTC", "100", "2026-03-31T00:01:00Z")
        )).isEmpty();
        assertThat(ruleEngine.evaluate(
                new BigDecimal("100"),
                Instant.parse("2026-03-31T00:01:00Z"),
                analysis("BTC", "101", "2026-03-31T00:01:00Z")
        )).isEmpty();
    }

    private AssetAnalysis analysis(String symbol, String currentPrice, String analyzedAt) {
        return new AssetAnalysis(
                symbol,
                "KRW",
                "UPBIT",
                new BigDecimal(currentPrice),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "WINDOW",
                Instant.parse(analyzedAt)
        );
    }
}
