package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertNotifierProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.webhooknotify.core.NotifyMessage;
import com.jean202.webhooknotify.core.channel.DiscordChannel;
import java.net.http.HttpClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class DiscordAssetAlertNotifier implements AssetAlertNotifier {
    private final AlertNotifierProperties.DiscordProperties properties;
    private final AssetAlertNotificationFormatter formatter;
    private final HttpClient httpClient;

    public DiscordAssetAlertNotifier(
            AlertNotifierProperties alertNotifierProperties,
            AssetAlertNotificationFormatter formatter
    ) {
        this.properties = alertNotifierProperties.getDiscord();
        this.formatter = formatter;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    @Override
    public boolean supports(AssetAlert alert) {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getWebhookUrl())
                && AlertSeverity.from(alert.severity())
                .map(severity -> severity.isAtLeast(properties.getMinimumSeverity()))
                .orElse(false);
    }

    @Override
    public Mono<Void> send(AssetAlert alert) {
        if (!supports(alert)) {
            return Mono.empty();
        }

        NotifyMessage message = NotifyMessage.of(
                "[%s] %s %s/%s".formatted(alert.severity(), alert.source(), alert.symbol(), alert.quoteCurrency()),
                formatter.format(alert)
        );
        // DiscordChannel.send blocks, so it must not run on the subscriber's thread:
        // boundedElastic keeps the returned Mono non-blocking for any caller. The channel
        // timeout is what actually frees that thread; the Mono-level timeout is only a
        // backstop, so it is given headroom to let the channel fail first.
        return Mono.<Void>fromRunnable(() -> new DiscordChannel(
                        properties.getWebhookUrl(), httpClient, properties.getTimeout()).send(message))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(properties.getTimeout().multipliedBy(2));
    }

    @Override
    public String notifierName() {
        return "discord";
    }
}
