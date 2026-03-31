package com.jean202.assetradar.analysis;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AssetAnalysisCalculator {
    public AssetAnalysis analyze(AssetPrice previousPrice, AssetPrice currentPrice) {
        if (previousPrice == null || previousPrice.price() == null || previousPrice.price().compareTo(BigDecimal.ZERO) == 0) {
            return new AssetAnalysis(
                    currentPrice.symbol(),
                    currentPrice.quoteCurrency(),
                    currentPrice.source(),
                    currentPrice.price(),
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "INITIAL",
                    currentPrice.collectedAt()
            );
        }

        BigDecimal priceChange = currentPrice.price().subtract(previousPrice.price());
        BigDecimal changeRate = priceChange.divide(previousPrice.price(), 8, RoundingMode.HALF_UP);

        return new AssetAnalysis(
                currentPrice.symbol(),
                currentPrice.quoteCurrency(),
                currentPrice.source(),
                currentPrice.price(),
                previousPrice.price(),
                priceChange,
                changeRate,
                movementOf(priceChange),
                currentPrice.collectedAt()
        );
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
