package com.jean202.assetradar.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.gold")
public class GoldCollectorProperties {
    private boolean enabled = true;
    private URI baseUrl = URI.create("https://api.gold-api.com");
    private String symbol = "XAU";
    private Duration refreshInterval = Duration.ofMinutes(5);
    private String source = "GOLDAPI";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String normalizedSymbol() {
        return normalize(symbol);
    }

    public String normalizedSource() {
        return normalize(source);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}
