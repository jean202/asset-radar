package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.api.ComparedAssetResponse;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindow;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssetComparatorTest {
    private final AssetComparator assetComparator = new AssetComparator();

    @Test
    void comparesWindowUsingStartAndEndPrices() {
        AssetCompareWindow window = new AssetCompareWindow(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                new AssetPrice("BTC", "KRW", "UPBIT", new BigDecimal("100"), BigDecimal.ZERO, Instant.parse("2026-03-01T00:00:00Z")),
                new AssetPrice("BTC", "KRW", "UPBIT", new BigDecimal("120"), BigDecimal.ZERO, Instant.parse("2026-03-31T00:00:00Z")),
                2
        );

        ComparedAssetResponse response = assetComparator.compare(window, new BigDecimal("1000000"));

        assertThat(response).isNotNull();
        assertThat(response.priceChange()).isEqualByComparingTo("20");
        assertThat(response.returnRate()).isEqualByComparingTo("0.20000000");
        assertThat(response.movement()).isEqualTo("UP");
        assertThat(response.projectedValue()).isEqualByComparingTo("1200000.00");
    }
}
