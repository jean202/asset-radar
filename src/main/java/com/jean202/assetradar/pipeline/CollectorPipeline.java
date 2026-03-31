package com.jean202.assetradar.pipeline;

import com.jean202.assetradar.collector.AssetCollector;
import com.jean202.assetradar.domain.AssetPrice;
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
    private final List<Disposable> subscriptions = new ArrayList<>();

    public CollectorPipeline(
            List<AssetCollector> collectors,
            AnalysisProcessor analysisProcessor,
            AssetPriceStore assetPriceStore,
            List<AssetPriceSink> sinks
    ) {
        this.collectors = collectors;
        this.analysisProcessor = analysisProcessor;
        this.assetPriceStore = assetPriceStore;
        this.sinks = sinks;
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

        Disposable subscription = analysisProcessor.passThrough(collector.collect())
                .concatMap(this::dispatch)
                .subscribe(
                        unused -> {
                        },
                        error -> log.error("Collector {} stopped with an error", collector.sourceName(), error)
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
