package com.jean202.assetradar.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.alert")
public class AlertProperties {
    private Duration window = Duration.ofMinutes(1);
    private BigDecimal infoChangeRateThreshold = new BigDecimal("0.005");
    private BigDecimal warnChangeRateThreshold = new BigDecimal("0.01");
    private BigDecimal criticalChangeRateThreshold = new BigDecimal("0.02");

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public BigDecimal getInfoChangeRateThreshold() {
        return infoChangeRateThreshold;
    }

    public void setInfoChangeRateThreshold(BigDecimal infoChangeRateThreshold) {
        this.infoChangeRateThreshold = infoChangeRateThreshold;
    }

    public BigDecimal getWarnChangeRateThreshold() {
        return warnChangeRateThreshold;
    }

    public void setWarnChangeRateThreshold(BigDecimal warnChangeRateThreshold) {
        this.warnChangeRateThreshold = warnChangeRateThreshold;
    }

    public BigDecimal getCriticalChangeRateThreshold() {
        return criticalChangeRateThreshold;
    }

    public void setCriticalChangeRateThreshold(BigDecimal criticalChangeRateThreshold) {
        this.criticalChangeRateThreshold = criticalChangeRateThreshold;
    }
}
