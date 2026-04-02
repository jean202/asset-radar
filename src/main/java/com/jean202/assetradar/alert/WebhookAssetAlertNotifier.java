package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertNotifierProperties;
import com.jean202.assetradar.domain.AssetAlert;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebhookAssetAlertNotifier implements AssetAlertNotifier {
    private final AlertNotifierProperties.WebhookProperties properties;
    private final AssetAlertNotificationFormatter formatter;
    private final WebClient webClient;

    public WebhookAssetAlertNotifier(
            AlertNotifierProperties alertNotifierProperties,
            AssetAlertNotificationFormatter formatter,
            WebClient.Builder webClientBuilder
    ) {
        this.properties = alertNotifierProperties.getWebhook();
        this.formatter = formatter;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public boolean supports(AssetAlert alert) {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getUrl())
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
                .uri(properties.getUrl())
                .headers(headers -> {
                    if (StringUtils.hasText(properties.getAuthToken())) {
                        headers.setBearerAuth(properties.getAuthToken());
                    }
                })
                .bodyValue(new AlertWebhookPayload(
                        alert.source(),
                        alert.symbol(),
                        alert.quoteCurrency(),
                        alert.alertType(),
                        alert.movement(),
                        formatter.percentChange(alert),
                        formatter.windowLabel(alert.windowSeconds()),
                        alert.windowSeconds(),
                        alert.severity(),
                        alert.baselineAt(),
                        alert.alertedAt(),
                        formatter.format(alert)
                ))
                .retrieve()
                .toBodilessEntity()
                .timeout(properties.getTimeout())
                .then();
    }

    @Override
    public String notifierName() {
        return "webhook";
    }

    private record AlertWebhookPayload(
            String source,
            String symbol,
            String quoteCurrency,
            String alertType,
            String movement,
            java.math.BigDecimal percentChange,
            String window,
            Long windowSeconds,
            String severity,
            Instant baselineAt,
            Instant alertedAt,
            String message
    ) {
    }
}
