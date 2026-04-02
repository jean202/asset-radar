package com.jean202.assetradar.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.AlertProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.domain.AssetAnalysis;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AssetAlertConsumerTest {
    @Test
    void persistsOnlyWhenAlertFingerprintChanges() {
        RecordingSink sink = new RecordingSink();
        RecordingNotifier notifier = new RecordingNotifier();
        AlertProperties alertProperties = new AlertProperties();
        AssetAlertConsumer consumer = new AssetAlertConsumer(
                alertProperties,
                new AssetAlertRuleEngine(alertProperties),
                List.of(sink),
                new AssetAlertNotificationDispatcher(List.of(notifier), Runnable::run),
                new SimpleMeterRegistry()
        );

        consumer.consume(analysis("BTC", "100", "2026-03-31T00:00:00Z"));
        consumer.consume(analysis("BTC", "101.0", "2026-03-31T00:00:30Z"));
        consumer.consume(analysis("BTC", "101.2", "2026-03-31T00:01:00Z"));
        consumer.consume(analysis("BTC", "102.5", "2026-03-31T00:01:30Z"));
        consumer.consume(analysis("BTC", "103.5", "2026-03-31T00:02:00Z"));

        assertThat(sink.persisted()).hasSize(2);
        assertThat(sink.persisted().get(0).severity()).isEqualTo("WARN");
        assertThat(sink.persisted().get(1).severity()).isEqualTo("CRITICAL");
        assertThat(notifier.sent()).hasSize(2);
    }

    @Test
    void reEmitsAfterAlertStateClears() {
        RecordingSink sink = new RecordingSink();
        RecordingNotifier notifier = new RecordingNotifier();
        AlertProperties alertProperties = new AlertProperties();
        AssetAlertConsumer consumer = new AssetAlertConsumer(
                alertProperties,
                new AssetAlertRuleEngine(alertProperties),
                List.of(sink),
                new AssetAlertNotificationDispatcher(List.of(notifier), Runnable::run),
                new SimpleMeterRegistry()
        );

        consumer.consume(analysis("BTC", "100", "2026-03-31T00:00:00Z"));
        consumer.consume(analysis("BTC", "101.2", "2026-03-31T00:01:00Z"));
        consumer.consume(analysis("BTC", "101.25", "2026-03-31T00:02:00Z"));
        consumer.consume(analysis("BTC", "102.6", "2026-03-31T00:03:00Z"));

        assertThat(sink.persisted()).hasSize(2);
        assertThat(sink.persisted()).extracting(AssetAlert::severity).containsOnly("WARN");
        assertThat(notifier.sent()).hasSize(2);
    }

    @Test
    void continuesPersistingAlertsWhenNotifierFails() {
        RecordingSink sink = new RecordingSink();
        AlertProperties alertProperties = new AlertProperties();
        AssetAlertConsumer consumer = new AssetAlertConsumer(
                alertProperties,
                new AssetAlertRuleEngine(alertProperties),
                List.of(sink),
                new AssetAlertNotificationDispatcher(List.of(new FailingNotifier()), Runnable::run),
                new SimpleMeterRegistry()
        );

        consumer.consume(analysis("BTC", "100", "2026-03-31T00:00:00Z"));
        consumer.consume(analysis("BTC", "101.2", "2026-03-31T00:01:00Z"));

        assertThat(sink.persisted()).hasSize(1);
        assertThat(sink.persisted().get(0).severity()).isEqualTo("WARN");
    }

    private AssetAnalysis analysis(String symbol, String currentPrice, String analyzedAt) {
        return new AssetAnalysis(
                symbol,
                "KRW",
                "UPBIT",
                new BigDecimal(currentPrice),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "WINDOW",
                Instant.parse(analyzedAt)
        );
    }

    private static final class RecordingSink implements AssetAlertSink {
        private final List<AssetAlert> persisted = new ArrayList<>();

        @Override
        public Mono<Void> persist(AssetAlert alert) {
            persisted.add(alert);
            return Mono.empty();
        }

        @Override
        public String sinkName() {
            return "recording";
        }

        List<AssetAlert> persisted() {
            return persisted;
        }
    }

    private static final class RecordingNotifier implements AssetAlertNotifier {
        private final List<AssetAlert> sent = new ArrayList<>();

        @Override
        public boolean supports(AssetAlert alert) {
            return true;
        }

        @Override
        public Mono<Void> send(AssetAlert alert) {
            sent.add(alert);
            return Mono.empty();
        }

        @Override
        public String notifierName() {
            return "recording";
        }

        List<AssetAlert> sent() {
            return sent;
        }
    }

    private static final class FailingNotifier implements AssetAlertNotifier {
        @Override
        public boolean supports(AssetAlert alert) {
            return true;
        }

        @Override
        public Mono<Void> send(AssetAlert alert) {
            return Mono.error(new IllegalStateException("boom"));
        }

        @Override
        public String notifierName() {
            return "failing";
        }
    }
}
