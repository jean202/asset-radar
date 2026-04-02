package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import java.time.Instant;
import java.util.List;

public record AssetHistoryResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        int count,
        List<AssetPrice> prices
) {
}
