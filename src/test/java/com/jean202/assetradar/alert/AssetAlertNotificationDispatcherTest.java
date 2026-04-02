package com.jean202.assetradar.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAlert;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AssetAlertNotificationDispatcherTest {
    @Test
    void dispatchesOnBackgroundExecutor() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BlockingNotifier notifier = new BlockingNotifier(started, release);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            AssetAlertNotificationDispatcher dispatcher =
                    new AssetAlertNotificationDispatcher(List.of(notifier), executor);

            long startedAt = System.nanoTime();
            dispatcher.dispatch(alert("CRITICAL"));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertThat(elapsedMillis).isLessThan(200L);
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(notifier.sentCount()).isEqualTo(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void continuesWhenOneNotifierFails() {
        RecordingNotifier notifier = new RecordingNotifier();
        AssetAlertNotificationDispatcher dispatcher = new AssetAlertNotificationDispatcher(
                List.of(new FailingNotifier(), notifier),
                Runnable::run
        );

        dispatcher.dispatch(alert("WARN"));

        assertThat(notifier.sent()).hasSize(1);
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

    private static final class BlockingNotifier implements AssetAlertNotifier {
        private final CountDownLatch started;
        private final CountDownLatch release;
        private int sentCount;

        private BlockingNotifier(CountDownLatch started, CountDownLatch release) {
            this.started = started;
            this.release = release;
        }

        @Override
        public boolean supports(AssetAlert alert) {
            return true;
        }

        @Override
        public Mono<Void> send(AssetAlert alert) {
            return Mono.fromRunnable(() -> {
                sentCount++;
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        @Override
        public String notifierName() {
            return "blocking";
        }

        int sentCount() {
            return sentCount;
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
