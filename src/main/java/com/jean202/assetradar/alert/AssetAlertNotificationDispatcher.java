package com.jean202.assetradar.alert;

import com.jean202.assetradar.domain.AssetAlert;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetAlertNotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AssetAlertNotificationDispatcher.class);

    private final List<AssetAlertNotifier> notifiers;
    private final Executor executor;

    public AssetAlertNotificationDispatcher(
            List<AssetAlertNotifier> notifiers,
            @Qualifier("assetAlertNotificationExecutor") Executor executor
    ) {
        this.notifiers = notifiers;
        this.executor = executor;
    }

    public void dispatch(AssetAlert alert) {
        for (AssetAlertNotifier notifier : notifiers) {
            if (!notifier.supports(alert)) {
                continue;
            }

            try {
                executor.execute(() -> send(notifier, alert));
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to schedule alert notifier {} for {}:{}:{}",
                        notifier.notifierName(),
                        alert.source(),
                        alert.quoteCurrency(),
                        alert.symbol(),
                        exception
                );
            }
        }
    }

    private void send(AssetAlertNotifier notifier, AssetAlert alert) {
        try {
            notifier.send(alert).onErrorResume(error -> {
                log.error(
                        "Alert notifier {} failed for {}:{}:{}",
                        notifier.notifierName(),
                        alert.source(),
                        alert.quoteCurrency(),
                        alert.symbol(),
                        error
                );
                return Mono.empty();
            }).block();
        } catch (RuntimeException exception) {
            log.error(
                    "Alert notifier {} threw unexpectedly for {}:{}:{}",
                    notifier.notifierName(),
                    alert.source(),
                    alert.quoteCurrency(),
                    alert.symbol(),
                    exception
            );
        }
    }
}
