package com.jean202.assetradar.query;

import com.jean202.assetradar.domain.AssetAlert;
import reactor.core.publisher.Flux;

public interface AssetAlertHistoryReader {
    Flux<AssetAlert> readHistory(AssetAlertHistoryQuery query);
}
