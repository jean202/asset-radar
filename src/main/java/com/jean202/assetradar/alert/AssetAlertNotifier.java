package com.jean202.assetradar.alert;

import com.jean202.assetradar.domain.AssetAlert;
import reactor.core.publisher.Mono;

public interface AssetAlertNotifier {
    boolean supports(AssetAlert alert);

    Mono<Void> send(AssetAlert alert);

    String notifierName();
}
