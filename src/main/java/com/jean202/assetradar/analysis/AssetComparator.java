package com.jean202.assetradar.analysis;

import com.jean202.assetradar.api.ComparedAssetResponse;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareWindow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AssetComparator {
    public ComparedAssetResponse compare(AssetCompareWindow window, BigDecimal baseAmount) {
        if (window == null || window.startPrice() == null || window.endPrice() == null) {
            return null;
        }

        AssetPrice startPrice = window.startPrice();
        AssetPrice endPrice = window.endPrice();
        if (startPrice.price() == null || endPrice.price() == null || startPrice.price().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal priceChange = endPrice.price().subtract(startPrice.price());
        BigDecimal returnRate = priceChange.divide(startPrice.price(), 8, RoundingMode.HALF_UP);
        BigDecimal projectedValue = projectValue(baseAmount, returnRate);

        return new ComparedAssetResponse(
                endPrice.symbol(),
                endPrice.source(),
                endPrice.quoteCurrency(),
                startPrice.collectedAt(),
                endPrice.collectedAt(),
                window.sampleCount(),
                startPrice.price(),
                endPrice.price(),
                priceChange,
                returnRate,
                movementOf(priceChange),
                projectedValue
        );
    }

    private BigDecimal projectValue(BigDecimal baseAmount, BigDecimal returnRate) {
        int scale = Math.max(baseAmount.scale(), 2);
        return baseAmount.multiply(BigDecimal.ONE.add(returnRate))
                .setScale(scale, RoundingMode.HALF_UP);
    }

    private String movementOf(BigDecimal priceChange) {
        int comparison = priceChange.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "UP";
        }
        if (comparison < 0) {
            return "DOWN";
        }
        return "FLAT";
    }
}
