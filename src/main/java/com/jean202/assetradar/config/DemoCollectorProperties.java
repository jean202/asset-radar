package com.jean202.assetradar.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.demo")
public class DemoCollectorProperties {
    private boolean enabled = false;
    private Duration refreshInterval = Duration.ofSeconds(2);
    private List<DemoAsset> assets = List.of(
            new DemoAsset("BTC", "Bitcoin", "KRW", "UPBIT", new BigDecimal("137500000"), new BigDecimal("0.018")),
            new DemoAsset("ETH", "Ethereum", "KRW", "UPBIT", new BigDecimal("5120000"), new BigDecimal("0.022")),
            new DemoAsset("005930", "Samsung Electronics", "KRW", "KIS", new BigDecimal("78500"), new BigDecimal("0.009")),
            new DemoAsset("NVDA", "NVIDIA", "USD", "ALPHAVANTAGE", new BigDecimal("890.20"), new BigDecimal("0.015")),
            new DemoAsset("XAU", "Gold Spot", "USD", "GOLDAPI", new BigDecimal("2350.50"), new BigDecimal("0.006"))
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public List<DemoAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<DemoAsset> assets) {
        this.assets = assets;
    }

    public List<DemoAsset> normalizedAssets() {
        if (assets == null) {
            return List.of();
        }

        return assets.stream()
                .filter(asset -> asset != null && asset.isUsable())
                .map(DemoAsset::normalizedCopy)
                .toList();
    }

    public static class DemoAsset {
        private String symbol;
        private String name;
        private String quoteCurrency;
        private String source;
        private BigDecimal basePrice;
        private BigDecimal changeAmplitude = new BigDecimal("0.01");

        public DemoAsset() {
        }

        public DemoAsset(
                String symbol,
                String name,
                String quoteCurrency,
                String source,
                BigDecimal basePrice,
                BigDecimal changeAmplitude
        ) {
            this.symbol = symbol;
            this.name = name;
            this.quoteCurrency = quoteCurrency;
            this.source = source;
            this.basePrice = basePrice;
            this.changeAmplitude = changeAmplitude;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getQuoteCurrency() {
            return quoteCurrency;
        }

        public void setQuoteCurrency(String quoteCurrency) {
            this.quoteCurrency = quoteCurrency;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public BigDecimal getChangeAmplitude() {
            return changeAmplitude;
        }

        public void setChangeAmplitude(BigDecimal changeAmplitude) {
            this.changeAmplitude = changeAmplitude;
        }

        boolean isUsable() {
            return !normalize(symbol).isBlank()
                    && !normalize(quoteCurrency).isBlank()
                    && !normalize(source).isBlank()
                    && basePrice != null
                    && basePrice.compareTo(BigDecimal.ZERO) > 0;
        }

        DemoAsset normalizedCopy() {
            return new DemoAsset(
                    normalize(symbol),
                    name == null ? "" : name.trim(),
                    normalize(quoteCurrency),
                    normalize(source),
                    basePrice,
                    changeAmplitude == null ? BigDecimal.ZERO : changeAmplitude
            );
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
