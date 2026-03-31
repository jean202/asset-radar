package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAlert;
import java.util.List;

public record AlertsResponse(
        String source,
        String quoteCurrency,
        int count,
        List<AssetAlert> alerts
) {
}
