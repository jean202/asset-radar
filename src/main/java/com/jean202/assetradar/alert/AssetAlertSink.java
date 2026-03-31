package com.jean202.assetradar.alert;

import com.jean202.assetradar.domain.AssetAlert;
import reactor.core.publisher.Mono;

public interface AssetAlertSink {
    Mono<Void> persist(AssetAlert alert);

    String sinkName();
}
