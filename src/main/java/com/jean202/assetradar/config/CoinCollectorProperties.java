package com.jean202.assetradar.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.coin")
public class CoinCollectorProperties {
    private boolean enabled = true;
    private URI websocketUrl = URI.create("wss://api.upbit.com/websocket/v1");
    private List<String> symbols = List.of("KRW-BTC", "KRW-ETH");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(URI websocketUrl) {
        this.websocketUrl = websocketUrl;
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
                        .distinct()
                        .toList();
    }
}
