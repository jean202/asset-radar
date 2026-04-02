package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAlert;
import reactor.core.publisher.Flux;

public interface LatestAssetAlertReader {
    Flux<AssetAlert> readLatest(LatestAssetAlertQuery query);
}
