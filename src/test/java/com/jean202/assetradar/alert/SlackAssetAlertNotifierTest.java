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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

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
                    formatter
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
                    formatter
            );

            notifier.send(alert("WARN")).block();

            assertThat(requests.poll(300, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotBlockSubscribingThreadWhileRequestIsInFlight() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        HttpServer server = stallingServer("/slack", received, release);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getSlack().setEnabled(true);
            properties.getSlack().setMinimumSeverity(AlertSeverity.WARN);
            properties.getSlack().setTimeout(Duration.ofSeconds(10));
            properties.getSlack().setWebhookUrl("http://localhost:%d/slack".formatted(server.getAddress().getPort()));
            SlackAssetAlertNotifier notifier = new SlackAssetAlertNotifier(properties, formatter);

            long startedAt = System.nanoTime();
            notifier.send(alert("WARN")).subscribe(ignored -> { }, error -> { });
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(elapsedMillis).isLessThan(500);
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    void failsWithTimeoutWhenWebhookStalls() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        HttpServer server = stallingServer("/slack", received, release);

        try {
            AlertNotifierProperties properties = new AlertNotifierProperties();
            properties.getSlack().setEnabled(true);
            properties.getSlack().setMinimumSeverity(AlertSeverity.WARN);
            properties.getSlack().setTimeout(Duration.ofMillis(200));
            properties.getSlack().setWebhookUrl("http://localhost:%d/slack".formatted(server.getAddress().getPort()));
            SlackAssetAlertNotifier notifier = new SlackAssetAlertNotifier(properties, formatter);

            StepVerifier.create(notifier.send(alert("WARN")))
                    .expectError(TimeoutException.class)
                    .verify(Duration.ofSeconds(5));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    private HttpServer stallingServer(String path, CountDownLatch received, CountDownLatch release)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            received.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        return server;
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
