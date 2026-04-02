package com.jean202.assetradar.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.collector.AssetCollector;
import com.jean202.assetradar.domain.AssetPrice;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class CollectorPipelineTest {
    @Test
    void storesAndFansOutCollectedPrices() {
        AssetPrice price = new AssetPrice(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000"),
                new BigDecimal("0.023"),
                Instant.parse("2026-03-31T00:00:00Z")
        );

        RecordingSink kafkaSink = new RecordingSink("kafka");
        RecordingSink redisSink = new RecordingSink("redis");
        RecordingSink postgresSink = new RecordingSink("postgres");
        AssetPriceStore store = new AssetPriceStore();

        CollectorPipeline pipeline = new CollectorPipeline(
                List.of(new StaticCollector(price)),
                new AnalysisProcessor(),
                store,
                List.of(kafkaSink, redisSink, postgresSink),
                new SimpleMeterRegistry()
        );

        pipeline.start();

        assertThat(store.snapshot()).containsExactly(price);
        assertThat(kafkaSink.persisted()).containsExactly(price);
        assertThat(redisSink.persisted()).containsExactly(price);
        assertThat(postgresSink.persisted()).containsExactly(price);

        pipeline.stop();
    }

    @Test
    void keepsProcessingWhenOneSinkFails() {
        AssetPrice price = new AssetPrice(
                "ETH",
                "KRW",
                "UPBIT",
                new BigDecimal("5120000"),
                new BigDecimal("-0.008"),
                Instant.parse("2026-03-31T00:00:05Z")
        );

        RecordingSink redisSink = new RecordingSink("redis");
        AssetPriceStore store = new AssetPriceStore();
        CollectorPipeline pipeline = new CollectorPipeline(
                List.of(new StaticCollector(price)),
                new AnalysisProcessor(),
                store,
                List.of(new FailingSink(), redisSink),
                new SimpleMeterRegistry()
        );

        pipeline.start();

        assertThat(store.snapshot()).containsExactly(price);
        assertThat(redisSink.persisted()).containsExactly(price);

        pipeline.stop();
    }

    private static final class StaticCollector implements AssetCollector {
        private final AssetPrice price;

        private StaticCollector(AssetPrice price) {
            this.price = price;
        }

        @Override
        public Flux<AssetPrice> collect() {
            return Flux.just(price);
        }

        @Override
        public String sourceName() {
            return "test";
        }
    }

    private static final class RecordingSink implements AssetPriceSink {
        private final String sinkName;
        private final List<AssetPrice> persisted = new ArrayList<>();

        private RecordingSink(String sinkName) {
            this.sinkName = sinkName;
        }

        @Override
        public Mono<Void> persist(AssetPrice price) {
            persisted.add(price);
            return Mono.empty();
        }

        @Override
        public String sinkName() {
            return sinkName;
        }

        List<AssetPrice> persisted() {
            return persisted;
        }
    }

    private static final class FailingSink implements AssetPriceSink {
        @Override
        public Mono<Void> persist(AssetPrice price) {
            return Mono.error(new IllegalStateException("boom"));
        }

        @Override
        public String sinkName() {
            return "failing";
        }
    }
}
