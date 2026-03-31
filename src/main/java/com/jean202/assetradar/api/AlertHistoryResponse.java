package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAlert;
import java.time.Instant;
import java.util.List;

public record AlertHistoryResponse(
        String symbol,
        String source,
        String quoteCurrency,
        Instant from,
        Instant to,
        int count,
        List<AssetAlert> alerts
) {
}
