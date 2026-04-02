package com.jean202.assetradar.alert;

import com.jean202.assetradar.config.AlertProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.domain.AssetAnalysis;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetAlertConsumer {
    private static final Logger log = LoggerFactory.getLogger(AssetAlertConsumer.class);

    private final AlertProperties alertProperties;
    private final AssetAlertRuleEngine assetAlertRuleEngine;
    private final List<AssetAlertSink> sinks;
    private final AssetAlertNotificationDispatcher notificationDispatcher;
    private final Counter alertsTriggeredCounter;
    private final Counter alertsEvaluatedCounter;
    private final ConcurrentMap<String, AlertFingerprint> latestAlerts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Deque<PriceWindowEntry>> priceWindows = new ConcurrentHashMap<>();

    public AssetAlertConsumer(
            AlertProperties alertProperties,
            AssetAlertRuleEngine assetAlertRuleEngine,
            List<AssetAlertSink> sinks,
            AssetAlertNotificationDispatcher notificationDispatcher,
            MeterRegistry meterRegistry
    ) {
        this.alertProperties = alertProperties;
        this.assetAlertRuleEngine = assetAlertRuleEngine;
        this.sinks = sinks;
        this.notificationDispatcher = notificationDispatcher;
        this.alertsTriggeredCounter = Counter.builder("asset.alerts.triggered")
                .description("Number of alerts triggered")
                .register(meterRegistry);
        this.alertsEvaluatedCounter = Counter.builder("asset.alerts.evaluated")
                .description("Number of alert evaluations performed")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${asset-radar.pipeline.analysis-kafka-topic}",
            groupId = "asset-radar-alert",
            containerFactory = "assetAnalysisKafkaListenerContainerFactory"
    )
    public void consume(AssetAnalysis analysis) {
        alertsEvaluatedCounter.increment();
        String key = keyOf(analysis);
        Optional<AssetAlert> candidate = evaluateWindowAlert(key, analysis);

        if (candidate.isEmpty()) {
            latestAlerts.remove(key);
            return;
        }

        AssetAlert alert = candidate.get();
        AlertFingerprint nextFingerprint = AlertFingerprint.from(alert);
        AlertFingerprint previousFingerprint = latestAlerts.put(key, nextFingerprint);
        if (nextFingerprint.equals(previousFingerprint)) {
            return;
        }

        alertsTriggeredCounter.increment();

        for (AssetAlertSink sink : sinks) {
            try {
                sink.persist(alert).onErrorResume(error -> {
                    log.error(
                            "Alert sink {} failed for {}:{}:{}",
                            sink.sinkName(),
                            alert.source(),
                            alert.quoteCurrency(),
                            alert.symbol(),
                            error
                    );
                    return Mono.empty();
                }).block();
            } catch (RuntimeException exception) {
                log.error(
                        "Alert sink {} threw unexpectedly for {}:{}:{}",
                        sink.sinkName(),
                        alert.source(),
                        alert.quoteCurrency(),
                        alert.symbol(),
                        exception
                );
            }
        }

        notificationDispatcher.dispatch(alert);
    }

    private Optional<AssetAlert> evaluateWindowAlert(String key, AssetAnalysis analysis) {
        if (analysis == null || analysis.currentPrice() == null || analysis.analyzedAt() == null) {
            return Optional.empty();
        }

        Deque<PriceWindowEntry> window = priceWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            window.addLast(new PriceWindowEntry(analysis.currentPrice(), analysis.analyzedAt()));
            pruneWindow(window, analysis.analyzedAt());

            PriceWindowEntry baseline = window.peekFirst();
            Instant cutoff = analysis.analyzedAt().minus(alertProperties.getWindow());
            if (baseline == null || baseline.observedAt().isAfter(cutoff) || !baseline.observedAt().isBefore(analysis.analyzedAt())) {
                return Optional.empty();
            }

            return assetAlertRuleEngine.evaluate(baseline.price(), baseline.observedAt(), analysis);
        }
    }

    private void pruneWindow(Deque<PriceWindowEntry> window, Instant currentAt) {
        Instant cutoff = currentAt.minus(alertProperties.getWindow());
        while (window.size() > 1) {
            PriceWindowEntry next = second(window);
            if (next == null || next.observedAt().isAfter(cutoff)) {
                return;
            }
            window.removeFirst();
        }
    }

    private PriceWindowEntry second(Deque<PriceWindowEntry> window) {
        var iterator = window.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        iterator.next();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private String keyOf(AssetAnalysis analysis) {
        return "%s:%s:%s".formatted(analysis.source(), analysis.quoteCurrency(), analysis.symbol());
    }

    private record AlertFingerprint(String alertType, String severity, String movement, BigDecimal thresholdRate) {
        private static AlertFingerprint from(AssetAlert alert) {
            return new AlertFingerprint(
                    alert.alertType(),
                    alert.severity(),
                    alert.movement(),
                    alert.thresholdRate()
            );
        }
    }

    private record PriceWindowEntry(BigDecimal price, Instant observedAt) {
    }
}
