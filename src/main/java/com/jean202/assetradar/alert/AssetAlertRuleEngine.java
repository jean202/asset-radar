package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.domain.AssetAnalysis;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AssetAlertRuleEngine {
    private final AlertProperties alertProperties;

    public AssetAlertRuleEngine(AlertProperties alertProperties) {
        this.alertProperties = alertProperties;
    }

    public Optional<AssetAlert> evaluate(BigDecimal baselinePrice, Instant baselineAt, AssetAnalysis currentAnalysis) {
        if (currentAnalysis == null
                || currentAnalysis.currentPrice() == null
                || currentAnalysis.analyzedAt() == null
                || baselinePrice == null
                || baselineAt == null
                || baselinePrice.compareTo(BigDecimal.ZERO) == 0
                || !baselineAt.isBefore(currentAnalysis.analyzedAt())) {
            return Optional.empty();
        }

        BigDecimal priceChange = currentAnalysis.currentPrice().subtract(baselinePrice);
        String movement = movementOf(priceChange);
        if ("FLAT".equals(movement)) {
            return Optional.empty();
        }

        BigDecimal changeRate = priceChange.divide(baselinePrice, 8, RoundingMode.HALF_UP);
        BigDecimal absoluteChangeRate = changeRate.abs();
        AlertLevel alertLevel = alertLevelOf(absoluteChangeRate);
        if (alertLevel == null) {
            return Optional.empty();
        }

        Duration window = alertProperties.getWindow();
        return Optional.of(new AssetAlert(
                currentAnalysis.symbol(),
                currentAnalysis.quoteCurrency(),
                currentAnalysis.source(),
                alertTypeOf(movement),
                alertLevel.severity(),
                movement,
                currentAnalysis.currentPrice(),
                baselinePrice,
                priceChange,
                changeRate,
                alertLevel.thresholdRate(),
                baselineAt,
                window.getSeconds(),
                messageOf(currentAnalysis, movement, changeRate, alertLevel, window),
                currentAnalysis.analyzedAt()
        ));
    }

    private AlertLevel alertLevelOf(BigDecimal absoluteChangeRate) {
        if (absoluteChangeRate.compareTo(alertProperties.getCriticalChangeRateThreshold()) >= 0) {
            return new AlertLevel("CRITICAL", alertProperties.getCriticalChangeRateThreshold());
        }

        if (absoluteChangeRate.compareTo(alertProperties.getWarnChangeRateThreshold()) >= 0) {
            return new AlertLevel("WARN", alertProperties.getWarnChangeRateThreshold());
        }

        if (absoluteChangeRate.compareTo(alertProperties.getInfoChangeRateThreshold()) >= 0) {
            return new AlertLevel("INFO", alertProperties.getInfoChangeRateThreshold());
        }

        return null;
    }

    private String alertTypeOf(String movement) {
        return "DOWN".equals(movement) ? "PRICE_DROP" : "PRICE_SURGE";
    }

    private String messageOf(
            AssetAnalysis currentAnalysis,
            String movement,
            BigDecimal changeRate,
            AlertLevel alertLevel,
            Duration window
    ) {
        return "%s %s/%s %s %.2f%% over %s (threshold %.2f%%)".formatted(
                currentAnalysis.source(),
                currentAnalysis.symbol(),
                currentAnalysis.quoteCurrency(),
                movement,
                percentOf(changeRate),
                windowLabel(window),
                percentOf(alertLevel.thresholdRate())
        );
    }

    private String movementOf(BigDecimal priceChange) {
        int comparison = priceChange.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "UP";
        }
        if (comparison < 0) {
            return "DOWN";
        }
        return "FLAT";
    }

    private double percentOf(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String windowLabel(Duration window) {
        long seconds = window.getSeconds();
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    private record AlertLevel(String severity, BigDecimal thresholdRate) {
    }
}
