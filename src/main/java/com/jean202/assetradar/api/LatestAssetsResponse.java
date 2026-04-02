package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import java.util.List;

public record LatestAssetsResponse(
        String source,
        String quoteCurrency,
        int count,
        List<AssetPrice> assets
) {
}
