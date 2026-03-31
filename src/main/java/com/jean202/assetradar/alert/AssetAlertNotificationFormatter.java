package com.jean202.assetradar.alert;

import com.jean202.assetradar.domain.AssetAlert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AssetAlertNotificationFormatter {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public String format(AssetAlert alert) {
        return "%s %s/%s %s %s over %s [%s] at %s".formatted(
                alert.source(),
                alert.symbol(),
                alert.quoteCurrency(),
                alert.movement(),
                percentLabel(alert),
                windowLabel(alert.windowSeconds()),
                alert.severity(),
                instantLabel(alert.alertedAt())
        );
    }

    public BigDecimal percentChange(AssetAlert alert) {
        if (alert == null || alert.changeRate() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return alert.changeRate()
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String percentLabel(AssetAlert alert) {
        return percentChange(alert).toPlainString() + "%";
    }

    public String windowLabel(Long windowSeconds) {
        if (windowSeconds == null || windowSeconds <= 0) {
            return "0s";
        }

        long seconds = windowSeconds;
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    public String instantLabel(Instant instant) {
        return instant == null ? "" : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
