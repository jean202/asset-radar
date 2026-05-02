package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertNotifierProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.webhooknotify.core.NotifyMessage;
import com.jean202.webhooknotify.core.channel.DiscordChannel;
import java.net.http.HttpClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

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
        return Mono.fromRunnable(() -> new DiscordChannel(properties.getWebhookUrl(), httpClient).send(message));
    }

    @Override
    public String notifierName() {
        return "discord";
    }
}
