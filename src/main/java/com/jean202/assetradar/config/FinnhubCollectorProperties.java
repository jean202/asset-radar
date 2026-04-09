package com.jean202.assetradar.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.finnhub")
public class FinnhubCollectorProperties {
    private boolean enabled = false;
    private URI baseUrl = URI.create("https://finnhub.io/api/v1");
    private String apiKey = "";
    private Duration refreshInterval = Duration.ofMinutes(1);
    private List<String> symbols = List.of("AAPL", "MSFT");

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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<String> normalizedSymbols() {
        return symbols == null
                ? List.of()
                : symbols.stream()
                        .map(String::trim)
                        .filter(symbol -> !symbol.isBlank())
                        .map(symbol -> symbol.toUpperCase(Locale.ROOT))
                        .distinct()
                        .toList();
    }
}
