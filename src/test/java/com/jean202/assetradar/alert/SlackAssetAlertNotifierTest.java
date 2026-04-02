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
import org.springframework.web.reactive.function.client.WebClient;

class SlackAssetAlertNotifierTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssetAlertNotificationFormatter formatter = new AssetAlertNotificationFormatter();

    @Test
    void sendsSlackWebhookPayload() throws Exception {
        BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
        HttpServer server = server("/slack", requests);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getSlack().setEnabled(true);
            properties.getSlack().setMinimumSeverity(AlertSeverity.WARN);
            properties.getSlack().setWebhookUrl("http://localhost:%d/slack".formatted(server.getAddress().getPort()));
            SlackAssetAlertNotifier notifier = new SlackAssetAlertNotifier(
                    properties,
                    formatter,
                    WebClient.builder()
            );

            notifier.send(alert("WARN")).block();

            CapturedRequest request = requests.poll(3, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.method()).isEqualTo("POST");
            JsonNode payload = objectMapper.readTree(request.body());
            assertThat(payload.get("text").asText())
                    .isEqualTo("UPBIT BTC/KRW UP 1.50% over 1m [WARN] at 2026-03-31T00:01:00Z");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void skipsSlackWebhookWhenSeverityIsBelowMinimum() throws Exception {
        BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();
        HttpServer server = server("/slack", requests);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getSlack().setEnabled(true);
            properties.getSlack().setMinimumSeverity(AlertSeverity.CRITICAL);
            properties.getSlack().setWebhookUrl("http://localhost:%d/slack".formatted(server.getAddress().getPort()));
            SlackAssetAlertNotifier notifier = new SlackAssetAlertNotifier(
                    properties,
                    formatter,
                    WebClient.builder()
            );

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
        requests.add(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                new String(body)
        ));
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

    private record CapturedRequest(String method, String path, String authorization, String body) {
    }
}
