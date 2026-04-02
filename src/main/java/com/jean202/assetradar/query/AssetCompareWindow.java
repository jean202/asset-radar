package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetPrice;

public record AssetCompareWindow(
        AssetCompareQuery.AssetSpec asset,
        AssetPrice startPrice,
        AssetPrice endPrice,
        long sampleCount
) {
}
