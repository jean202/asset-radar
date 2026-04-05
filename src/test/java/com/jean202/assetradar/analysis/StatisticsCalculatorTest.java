package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatisticsCalculatorTest {
    private final StatisticsCalculator calculator = new StatisticsCalculator();

    private static List<BigDecimal> decimals(double... values) {
        return java.util.Arrays.stream(values)
                .mapToObj(BigDecimal::valueOf)
                .toList();
    }

    // --- SMA ---

    @Test
    void sma_computesCorrectly() {
        List<BigDecimal> prices = decimals(10, 20, 30, 40, 50);
        List<BigDecimal> result = calculator.sma(prices, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).doubleValue()).isCloseTo(20.0, within(0.0001));  // (10+20+30)/3
        assertThat(result.get(1).doubleValue()).isCloseTo(30.0, within(0.0001));  // (20+30+40)/3
        assertThat(result.get(2).doubleValue()).isCloseTo(40.0, within(0.0001));  // (30+40+50)/3
    }

    @Test
    void sma_windowLargerThanData_returnsEmpty() {
        assertThat(calculator.sma(decimals(10, 20), 5)).isEmpty();
    }

    // --- EMA ---

    @Test
    void ema_firstValueEqualsFirstPrice() {
        List<BigDecimal> prices = decimals(10, 20, 30);
        List<BigDecimal> result = calculator.ema(prices, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).doubleValue()).isCloseTo(10.0, within(0.0001));
    }

    @Test
    void ema_emptyInput_returnsEmpty() {
        assertThat(calculator.ema(List.of(), 5)).isEmpty();
    }

    // --- Standard Deviation ---

    @Test
    void stdDev_knownValues() {
        // Population stddev of [2, 4, 4, 4, 5, 5, 7, 9] = 2.0
        List<BigDecimal> values = decimals(2, 4, 4, 4, 5, 5, 7, 9);
        BigDecimal result = calculator.standardDeviation(values);
        assertThat(result.doubleValue()).isCloseTo(2.0, within(0.001));
    }

    @Test
    void stdDev_singleValue_returnsZero() {
        assertThat(calculator.standardDeviation(decimals(5))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Returns ---

    @Test
    void returns_computesPercentageChange() {
        List<BigDecimal> prices = decimals(100, 110, 99);
        List<BigDecimal> result = calculator.returns(prices);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).doubleValue()).isCloseTo(0.10, within(0.0001));  // +10%
        assertThat(result.get(1).doubleValue()).isCloseTo(-0.10, within(0.001));  // -10%
    }

    @Test
    void returns_singlePrice_returnsEmpty() {
        assertThat(calculator.returns(decimals(100))).isEmpty();
    }

    // --- Pearson Correlation ---

    @Test
    void pearson_perfectPositive() {
        List<BigDecimal> x = decimals(1, 2, 3, 4, 5);
        List<BigDecimal> y = decimals(2, 4, 6, 8, 10);
        BigDecimal result = calculator.pearsonCorrelation(x, y);
        assertThat(result.doubleValue()).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void pearson_perfectNegative() {
        List<BigDecimal> x = decimals(1, 2, 3, 4, 5);
        List<BigDecimal> y = decimals(10, 8, 6, 4, 2);
        BigDecimal result = calculator.pearsonCorrelation(x, y);
        assertThat(result.doubleValue()).isCloseTo(-1.0, within(0.0001));
    }

    @Test
    void pearson_unequalSize_returnsZero() {
        assertThat(calculator.pearsonCorrelation(decimals(1, 2), decimals(1))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Summarize ---

    @Test
    void summarize_computesAllFields() {
        List<BigDecimal> values = decimals(10, 20, 30, 40, 50);
        SummaryStatistics stats = calculator.summarize(values);

        assertThat(stats.count()).isEqualTo(5);
        assertThat(stats.min().doubleValue()).isCloseTo(10.0, within(0.001));
        assertThat(stats.max().doubleValue()).isCloseTo(50.0, within(0.001));
        assertThat(stats.mean().doubleValue()).isCloseTo(30.0, within(0.001));
        assertThat(stats.median().doubleValue()).isCloseTo(30.0, within(0.001));
    }

    @Test
    void summarize_empty_returnsZeros() {
        SummaryStatistics stats = calculator.summarize(List.of());
        assertThat(stats.count()).isZero();
        assertThat(stats.mean()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Max Drawdown ---

    @Test
    void maxDrawdown_computesCorrectly() {
        // Peak at 100, drops to 60 = -40%
        List<BigDecimal> prices = decimals(80, 100, 90, 60, 70);
        BigDecimal result = calculator.maxDrawdown(prices);
        assertThat(result.doubleValue()).isCloseTo(-0.40, within(0.001));
    }

    @Test
    void maxDrawdown_monotonicallyIncreasing_returnsZero() {
        List<BigDecimal> prices = decimals(10, 20, 30, 40);
        BigDecimal result = calculator.maxDrawdown(prices);
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Rolling Std Dev ---

    @Test
    void rollingStdDev_computesCorrectWindowCount() {
        List<BigDecimal> values = decimals(1, 2, 3, 4, 5, 6);
        List<BigDecimal> result = calculator.rollingStdDev(values, 3);
        assertThat(result).hasSize(4); // 6 - 3 + 1
    }
}
