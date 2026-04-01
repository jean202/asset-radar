package com.jean202.assetradar.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.dashboard")
public class DashboardProperties {
    private Duration defaultStaleThreshold = Duration.ofMinutes(10);
    private Map<String, Duration> staleThresholds = defaultThresholds();

    public Duration getDefaultStaleThreshold() {
        return defaultStaleThreshold;
    }

    public void setDefaultStaleThreshold(Duration defaultStaleThreshold) {
        this.defaultStaleThreshold = defaultStaleThreshold;
    }

    public Map<String, Duration> getStaleThresholds() {
        return staleThresholds;
    }

    public void setStaleThresholds(Map<String, Duration> staleThresholds) {
        this.staleThresholds = staleThresholds == null ? new LinkedHashMap<>() : new LinkedHashMap<>(staleThresholds);
    }

    public Duration staleThresholdFor(String source) {
        if (source == null || source.isBlank()) {
            return defaultStaleThreshold;
        }

        return staleThresholds.getOrDefault(normalize(source), defaultStaleThreshold);
    }

    private static Map<String, Duration> defaultThresholds() {
        Map<String, Duration> thresholds = new LinkedHashMap<>();
        thresholds.put("UPBIT", Duration.ofSeconds(30));
        thresholds.put("BINANCE", Duration.ofSeconds(30));
        thresholds.put("GOLDAPI", Duration.ofMinutes(15));
        thresholds.put("KIS", Duration.ofMinutes(1));
        thresholds.put("KOREAINVESTMENT", Duration.ofMinutes(1));
        thresholds.put("ALPHAVANTAGE", Duration.ofMinutes(20));
        thresholds.put("FINNHUB", Duration.ofMinutes(20));
        return thresholds;
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
