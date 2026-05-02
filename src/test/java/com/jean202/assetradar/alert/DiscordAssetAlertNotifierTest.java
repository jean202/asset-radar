package com.jean202.assetradar.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.AlertNotifierProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DiscordAssetAlertNotifierTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssetAlertNotificationFormatter formatter = new AssetAlertNotificationFormatter();

    @Test
    void sendsDiscordWebhookPayload() throws Exception {
        BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
        HttpServer server = server("/discord", requests);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getDiscord().setEnabled(true);
            properties.getDiscord().setMinimumSeverity(AlertSeverity.WARN);
            properties.getDiscord().setWebhookUrl("http://localhost:%d/discord".formatted(server.getAddress().getPort()));
            DiscordAssetAlertNotifier notifier = new DiscordAssetAlertNotifier(properties, formatter);

            notifier.send(alert("WARN")).block();

            CapturedRequest request = requests.poll(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.method()).isEqualTo("POST");
            JsonNode payload = objectMapper.readTree(request.body());
            assertThat(payload.get("embeds")).isNotNull();
            assertThat(payload.get("embeds")).hasSize(1);
            assertThat(payload.get("embeds").get(0).get("title").asText())
                    .isEqualTo("[WARN] UPBIT BTC/KRW");
            assertThat(payload.get("embeds").get(0).get("description").asText())
                    .isEqualTo("UPBIT BTC/KRW UP 1.50% over 1m [WARN] at 2026-03-31T00:01:00Z");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void skipsDiscordWebhookWhenSeverityIsBelowMinimum() throws Exception {
        BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
        HttpServer server = server("/discord", requests);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getDiscord().setEnabled(true);
            properties.getDiscord().setMinimumSeverity(AlertSeverity.CRITICAL);
            properties.getDiscord().setWebhookUrl("http://localhost:%d/discord".formatted(server.getAddress().getPort()));
            DiscordAssetAlertNotifier notifier = new DiscordAssetAlertNotifier(properties, formatter);

            notifier.send(alert("WARN")).block();

            assertThat(requests.poll(300, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            server.stop(0);
        }
    }

    private HttpServer server(String path, BlockingQueue<CapturedRequest> requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> handle(exchange, requests));
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange, BlockingQueue<CapturedRequest> requests) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        requests.add(new CapturedRequest(exchange.getRequestMethod(), new String(body)));
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private AssetAlert alert(String severity) {
        return new AssetAlert(
                "BTC",
                "KRW",
                "UPBIT",
                "PRICE_SURGE",
                severity,
                "UP",
                new BigDecimal("101.5"),
                new BigDecimal("100"),
                new BigDecimal("1.5"),
                new BigDecimal("0.015"),
                new BigDecimal("0.01"),
                Instant.parse("2026-03-31T00:00:00Z"),
                60L,
                "message",
                Instant.parse("2026-03-31T00:01:00Z")
        );
    }

    private record CapturedRequest(String method, String body) {
    }
}
