package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertNotifierProperties;
import com.jean202.assetradar.domain.AssetAlert;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SlackAssetAlertNotifier implements AssetAlertNotifier {
    private final AlertNotifierProperties.SlackProperties properties;
    private final AssetAlertNotificationFormatter formatter;
    private final WebClient webClient;

    public SlackAssetAlertNotifier(
            AlertNotifierProperties alertNotifierProperties,
            AssetAlertNotificationFormatter formatter,
            WebClient.Builder webClientBuilder
    ) {
        this.properties = alertNotifierProperties.getSlack();
        this.formatter = formatter;
        this.webClient = webClientBuilder.build();
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

        return webClient.post()
                .uri(properties.getWebhookUrl())
                .bodyValue(new SlackWebhookPayload(formatter.format(alert)))
                .retrieve()
                .toBodilessEntity()
                .timeout(properties.getTimeout())
                .then();
    }

    @Override
    public String notifierName() {
        return "slack";
    }

    private record SlackWebhookPayload(String text) {
    }
}
