package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssetAnalysisCalculatorTest {
    private final AssetAnalysisCalculator calculator = new AssetAnalysisCalculator();

    @Test
    void returnsInitialWhenPreviousPriceIsMissing() {
        AssetAnalysis analysis = calculator.analyze(null, assetPrice("BTC", "100", "2026-03-31T00:00:00Z"));

        assertThat(analysis.movement()).isEqualTo("INITIAL");
        assertThat(analysis.previousPrice()).isNull();
        assertThat(analysis.priceChange()).isEqualByComparingTo("0");
        assertThat(analysis.changeRate()).isEqualByComparingTo("0");
    }

    @Test
    void calculatesUpwardMovement() {
        AssetAnalysis analysis = calculator.analyze(
                assetPrice("BTC", "100", "2026-03-31T00:00:00Z"),
                assetPrice("BTC", "110", "2026-03-31T00:00:01Z")
        );

        assertThat(analysis.movement()).isEqualTo("UP");
        assertThat(analysis.priceChange()).isEqualByComparingTo("10");
        assertThat(analysis.changeRate()).isEqualByComparingTo("0.10000000");
    }

    @Test
    void calculatesDownwardMovement() {
        AssetAnalysis analysis = calculator.analyze(
                assetPrice("BTC", "110", "2026-03-31T00:00:00Z"),
                assetPrice("BTC", "100", "2026-03-31T00:00:01Z")
        );

        assertThat(analysis.movement()).isEqualTo("DOWN");
        assertThat(analysis.priceChange()).isEqualByComparingTo("-10");
        assertThat(analysis.changeRate()).isEqualByComparingTo("-0.09090909");
    }

    private AssetPrice assetPrice(String symbol, String price, String collectedAt) {
        return new AssetPrice(
                symbol,
                "KRW",
                "UPBIT",
                new BigDecimal(price),
                BigDecimal.ZERO,
                Instant.parse(collectedAt)
        );
    }
}
