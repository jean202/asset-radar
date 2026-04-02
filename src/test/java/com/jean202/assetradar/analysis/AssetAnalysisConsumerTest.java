package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetPrice;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AssetAnalysisConsumerTest {
    @Test
    void consumesPricesAndPersistsAnalysis() {
        RecordingSink sink = new RecordingSink();
        AssetAnalysisConsumer consumer = new AssetAnalysisConsumer(new AssetAnalysisCalculator(), List.of(sink), new SimpleMeterRegistry());

        consumer.consume(assetPrice("BTC", "100", "2026-03-31T00:00:00Z"));
        consumer.consume(assetPrice("BTC", "105", "2026-03-31T00:00:01Z"));

        assertThat(sink.persisted()).hasSize(2);
        assertThat(sink.persisted().get(0).movement()).isEqualTo("INITIAL");
        assertThat(sink.persisted().get(1).movement()).isEqualTo("UP");
        assertThat(sink.persisted().get(1).priceChange()).isEqualByComparingTo("5");
    }

    private AssetPrice assetPrice(String symbol, String price, String collectedAt) {
        return new AssetPrice(
                symbol,
                "KRW",
                "UPBIT",
                new BigDecimal(price),
                BigDecimal.ZERO,
                Instant.parse(collectedAt)
        );
    }

    private static final class RecordingSink implements AssetAnalysisSink {
        private final List<AssetAnalysis> persisted = new ArrayList<>();

        @Override
        public Mono<Void> persist(AssetAnalysis analysis) {
            persisted.add(analysis);
            return Mono.empty();
        }

        @Override
        public String sinkName() {
            return "recording";
        }

        List<AssetAnalysis> persisted() {
            return persisted;
        }
    }
}
