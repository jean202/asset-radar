package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        String status,
        Instant updatedAt,
        int assetCount,
        long historyRowCount,
        Instant historyUpdatedAt,
        List<AssetPrice> assets
) {
}
