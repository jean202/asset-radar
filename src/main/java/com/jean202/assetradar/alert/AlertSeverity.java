package com.jean202.assetradar.alert;

import java.util.Locale;
import java.util.Optional;

public enum AlertSeverity {
    INFO,
    WARN,
    CRITICAL;

    public boolean isAtLeast(AlertSeverity minimumSeverity) {
        return this.ordinal() >= minimumSeverity.ordinal();
    }

    public static Optional<AlertSeverity> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(AlertSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
