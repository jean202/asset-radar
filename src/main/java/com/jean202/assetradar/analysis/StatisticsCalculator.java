package com.jean202.assetradar.analysis;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StatisticsCalculator {
    private static final int SCALE = 8;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MC = new MathContext(20, ROUNDING);

    public List<BigDecimal> sma(List<BigDecimal> prices, int window) {
        if (prices.size() < window) {
            return List.of();
        }

        List<BigDecimal> result = new ArrayList<>(prices.size() - window + 1);
        BigDecimal windowSize = BigDecimal.valueOf(window);
        BigDecimal sum = BigDecimal.ZERO;

        for (int i = 0; i < window; i++) {
            sum = sum.add(prices.get(i));
        }
        result.add(sum.divide(windowSize, SCALE, ROUNDING));

        for (int i = window; i < prices.size(); i++) {
            sum = sum.add(prices.get(i)).subtract(prices.get(i - window));
            result.add(sum.divide(windowSize, SCALE, ROUNDING));
        }

        return result;
    }

    public List<BigDecimal> ema(List<BigDecimal> prices, int window) {
        if (prices.isEmpty()) {
            return List.of();
        }

        BigDecimal multiplier = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(window + 1L), SCALE, ROUNDING);
        BigDecimal complement = BigDecimal.ONE.subtract(multiplier);

        List<BigDecimal> result = new ArrayList<>(prices.size());
        result.add(prices.get(0));

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal emaValue = prices.get(i).multiply(multiplier, MC)
                    .add(result.get(i - 1).multiply(complement, MC))
                    .setScale(SCALE, ROUNDING);
            result.add(emaValue);
        }

        return result;
    }

    public BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = mean(values);
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;

        for (BigDecimal value : values) {
            BigDecimal diff = value.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff, MC));
        }

        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(values.size()), MC);
        return sqrt(variance);
    }

    public List<BigDecimal> rollingStdDev(List<BigDecimal> values, int window) {
        if (values.size() < window) {
            return List.of();
        }

        List<BigDecimal> result = new ArrayList<>(values.size() - window + 1);
        for (int i = 0; i <= values.size() - window; i++) {
            result.add(standardDeviation(values.subList(i, i + window)));
        }
        return result;
    }

    public List<BigDecimal> returns(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return List.of();
        }

        List<BigDecimal> result = new ArrayList<>(prices.size() - 1);
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal prev = prices.get(i - 1);
            if (prev.compareTo(BigDecimal.ZERO) == 0) {
                result.add(BigDecimal.ZERO);
            } else {
                result.add(prices.get(i).subtract(prev)
                        .divide(prev, SCALE, ROUNDING));
            }
        }
        return result;
    }

    public BigDecimal pearsonCorrelation(List<BigDecimal> x, List<BigDecimal> y) {
        if (x.size() != y.size() || x.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal meanX = mean(x);
        BigDecimal meanY = mean(y);

        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumX2 = BigDecimal.ZERO;
        BigDecimal sumY2 = BigDecimal.ZERO;

        for (int i = 0; i < x.size(); i++) {
            BigDecimal dx = x.get(i).subtract(meanX);
            BigDecimal dy = y.get(i).subtract(meanY);
            sumXY = sumXY.add(dx.multiply(dy, MC));
            sumX2 = sumX2.add(dx.multiply(dx, MC));
            sumY2 = sumY2.add(dy.multiply(dy, MC));
        }

        BigDecimal denominator = sqrt(sumX2.multiply(sumY2, MC));
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return sumXY.divide(denominator, SCALE, ROUNDING);
    }

    public SummaryStatistics summarize(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return new SummaryStatistics(0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        return new SummaryStatistics(
                sorted.size(),
                sorted.get(0),
                sorted.get(sorted.size() - 1),
                mean(values),
                standardDeviation(values),
                percentile(sorted, 50),
                percentile(sorted, 5),
                percentile(sorted, 25),
                percentile(sorted, 75),
                percentile(sorted, 95)
        );
    }

    public BigDecimal maxDrawdown(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal peak = prices.get(0);
        BigDecimal maxDd = BigDecimal.ZERO;

        for (BigDecimal price : prices) {
            if (price.compareTo(peak) > 0) {
                peak = price;
            }
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dd = price.subtract(peak).divide(peak, SCALE, ROUNDING);
                if (dd.compareTo(maxDd) < 0) {
                    maxDd = dd;
                }
            }
        }

        return maxDd;
    }

    private BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING);
    }

    private BigDecimal percentile(List<BigDecimal> sorted, int p) {
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double rank = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = Math.min(lower + 1, sorted.size() - 1);
        BigDecimal fraction = BigDecimal.valueOf(rank - lower);
        return sorted.get(lower).add(
                sorted.get(upper).subtract(sorted.get(lower)).multiply(fraction, MC)
        ).setScale(SCALE, ROUNDING);
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.sqrt(MC).setScale(SCALE, ROUNDING);
    }
}
