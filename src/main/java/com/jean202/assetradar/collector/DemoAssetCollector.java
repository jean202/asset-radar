package com.jean202.assetradar.collector;

import com.jean202.assetradar.config.DemoCollectorProperties;
import com.jean202.assetradar.config.DemoCollectorProperties.DemoAsset;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DemoAssetCollector implements AssetCollector {
    private static final Logger log = LoggerFactory.getLogger(DemoAssetCollector.class);
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(2);

    private final DemoCollectorProperties properties;

    public DemoAssetCollector(DemoCollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public Flux<AssetPrice> collect() {
        List<DemoAsset> assets = properties.normalizedAssets();
        if (!properties.isEnabled() || assets.isEmpty()) {
            log.info("Demo collector is disabled or no assets are configured.");
            return Flux.empty();
        }

        Duration refreshInterval = validRefreshInterval();
        log.info("Starting demo collector for {} assets every {}", assets.size(), refreshInterval);

        return Flux.interval(Duration.ZERO, refreshInterval)
                .flatMapIterable(tick -> pricesForTick(tick, Instant.now(), assets));
    }

    @Override
    public String sourceName() {
        return "demo";
    }

    List<AssetPrice> pricesForTick(long tick, Instant collectedAt, List<DemoAsset> assets) {
        return assets.stream()
                .map(asset -> priceForTick(asset, tick, collectedAt))
                .toList();
    }

    private AssetPrice priceForTick(DemoAsset asset, long tick, Instant collectedAt) {
        BigDecimal changeRate = changeRateFor(asset, tick);
        BigDecimal price = asset.getBasePrice()
                .multiply(BigDecimal.ONE.add(changeRate))
                .setScale(priceScale(asset.getQuoteCurrency()), RoundingMode.HALF_UP);

        return new AssetPrice(
                asset.getSymbol(),
                asset.getName(),
                asset.getQuoteCurrency(),
                asset.getSource(),
                price,
                changeRate,
                collectedAt
        );
    }

    private BigDecimal changeRateFor(DemoAsset asset, long tick) {
        double phase = (Integer.toUnsignedLong(asset.getSymbol().hashCode()) % 360) / 45.0;
        double wave = Math.sin((tick + phase) / 4.0) * asset.getChangeAmplitude().doubleValue();
        return BigDecimal.valueOf(wave).setScale(6, RoundingMode.HALF_UP);
    }

    private int priceScale(String quoteCurrency) {
        return "KRW".equals(quoteCurrency) ? 0 : 2;
    }

    private Duration validRefreshInterval() {
        Duration configured = properties.getRefreshInterval();
        if (configured == null || configured.isZero() || configured.isNegative()) {
            return DEFAULT_REFRESH_INTERVAL;
        }
        return configured;
    }
}
