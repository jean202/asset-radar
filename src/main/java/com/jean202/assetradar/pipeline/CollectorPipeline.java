package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.collector.AssetCollector;
import com.jean202.assetradar.domain.AssetPrice;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CollectorPipeline {
    private static final Logger log = LoggerFactory.getLogger(CollectorPipeline.class);

    private final List<AssetCollector> collectors;
    private final AnalysisProcessor analysisProcessor;
    private final AssetPriceStore assetPriceStore;
    private final List<AssetPriceSink> sinks;
    private final MeterRegistry meterRegistry;
    private final List<Disposable> subscriptions = new ArrayList<>();

    public CollectorPipeline(
            List<AssetCollector> collectors,
            AnalysisProcessor analysisProcessor,
            AssetPriceStore assetPriceStore,
            List<AssetPriceSink> sinks,
            MeterRegistry meterRegistry
    ) {
        this.collectors = collectors;
        this.analysisProcessor = analysisProcessor;
        this.assetPriceStore = assetPriceStore;
        this.sinks = sinks;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void start() {
        collectors.forEach(this::subscribe);
    }

    @PreDestroy
    void stop() {
        subscriptions.forEach(Disposable::dispose);
    }

    private void subscribe(AssetCollector collector) {
        log.info("Starting collector {}", collector.sourceName());

        Counter priceCounter = Counter.builder("asset.collector.prices")
                .tag("source", collector.sourceName())
                .description("Number of prices collected")
                .register(meterRegistry);

        Counter errorCounter = Counter.builder("asset.collector.errors")
                .tag("source", collector.sourceName())
                .description("Number of collector errors")
                .register(meterRegistry);

        Disposable subscription = analysisProcessor.passThrough(collector.collect())
                .doOnNext(price -> priceCounter.increment())
                .concatMap(this::dispatch)
                .subscribe(
                        unused -> {
                        },
                        error -> {
                            errorCounter.increment();
                            log.error("Collector {} stopped with an error", collector.sourceName(), error);
                        }
                );

        subscriptions.add(subscription);
    }

    private Mono<Void> dispatch(AssetPrice price) {
        assetPriceStore.update(price);

        if (sinks.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(sinks)
                .flatMap(sink -> sink.persist(price)
                                .onErrorResume(error -> {
                                    log.error(
                                            "Sink {} failed for {}:{}:{}",
                                            sink.sinkName(),
                                            price.source(),
                                            price.quoteCurrency(),
                                            price.symbol(),
                                            error
                                    );
                                    return Mono.empty();
                                }),
                        sinks.size())
                .then();
    }
}
