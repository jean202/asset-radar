package com.jean202.assetradar.config;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.stock-kr")
public class StockKrCollectorProperties {
    private boolean enabled = true;
    private URI baseUrl = URI.create("https://openapi.koreainvestment.com:9443");
    private String appKey = "";
    private String appSecret = "";
    private Duration refreshInterval = Duration.ofSeconds(3);
    private List<String> symbols = List.of("005930", "000660");
    private Map<String, String> symbolNames = new HashMap<>();
    private String source = "KIS";

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

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
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

    public Map<String, String> getSymbolNames() {
        return symbolNames;
    }

    public void setSymbolNames(Map<String, String> symbolNames) {
        this.symbolNames = symbolNames;
    }

    public String nameFor(String symbol) {
        return symbolNames.getOrDefault(symbol, "");
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> normalizedSymbols() {
        return symbols == null
                ? List.of()
                : symbols.stream()
                        .map(String::trim)
                        .filter(symbol -> !symbol.isBlank())
                        .distinct()
                        .toList();
    }

    public String normalizedSource() {
        if (source == null) {
            return "";
        }
        return source.trim().toUpperCase(Locale.ROOT);
    }
}
