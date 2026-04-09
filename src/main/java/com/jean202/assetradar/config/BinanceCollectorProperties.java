package com.jean202.assetradar.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.binance")
public class BinanceCollectorProperties {
    private boolean enabled = false;
    private URI websocketUrl = URI.create("wss://stream.binance.com:9443/ws");
    private List<String> symbols = List.of("btcusdt", "ethusdt");

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
                        .map(symbol -> symbol.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
    }
}
