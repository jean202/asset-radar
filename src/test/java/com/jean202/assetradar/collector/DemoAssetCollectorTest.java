package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.DemoCollectorProperties;
import com.jean202.assetradar.config.DemoCollectorProperties.DemoAsset;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DemoAssetCollectorTest {
    @Test
    void emitsConfiguredDemoPricesImmediately() {
        DemoCollectorProperties properties = new DemoCollectorProperties();
        properties.setEnabled(true);
        properties.setRefreshInterval(Duration.ofSeconds(10));
        properties.setAssets(List.of(new DemoAsset(
                "btc",
                "Bitcoin",
                "krw",
                "upbit",
                new BigDecimal("100000000"),
                new BigDecimal("0.01")
        )));

        DemoAssetCollector collector = new DemoAssetCollector(properties);

        StepVerifier.create(collector.collect().take(1))
                .assertNext(price -> {
                    assertThat(price.symbol()).isEqualTo("BTC");
                    assertThat(price.name()).isEqualTo("Bitcoin");
                    assertThat(price.quoteCurrency()).isEqualTo("KRW");
                    assertThat(price.source()).isEqualTo("UPBIT");
                    assertThat(price.price()).isPositive();
                    assertThat(price.collectedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void staysIdleWhenDisabled() {
        DemoCollectorProperties properties = new DemoCollectorProperties();
        properties.setEnabled(false);

        DemoAssetCollector collector = new DemoAssetCollector(properties);

        StepVerifier.create(collector.collect())
                .verifyComplete();
    }
}
