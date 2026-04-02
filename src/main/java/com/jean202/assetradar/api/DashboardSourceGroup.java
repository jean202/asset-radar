package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import java.time.Instant;
import java.util.List;

public record DashboardSourceGroup(
        String source,
        String sourceDisplayName,
        String assetType,
        String assetTypeLabel,
        Instant updatedAt,
        boolean stale,
        long lastUpdatedAgeSeconds,
        String lastUpdatedAgo,
        int assetCount,
        List<AssetPrice> assets
) {
}
