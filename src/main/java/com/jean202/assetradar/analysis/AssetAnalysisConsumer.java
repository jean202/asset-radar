package com.jean202.assetradar.analysis;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetPrice;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetAnalysisConsumer {
    private static final Logger log = LoggerFactory.getLogger(AssetAnalysisConsumer.class);

    private final AssetAnalysisCalculator assetAnalysisCalculator;
    private final List<AssetAnalysisSink> sinks;
    private final ConcurrentMap<String, AssetPrice> latestPrices = new ConcurrentHashMap<>();

    public AssetAnalysisConsumer(
            AssetAnalysisCalculator assetAnalysisCalculator,
            List<AssetAnalysisSink> sinks
    ) {
        this.assetAnalysisCalculator = assetAnalysisCalculator;
        this.sinks = sinks;
    }

    @KafkaListener(
            topics = "${asset-radar.pipeline.kafka-topic}",
            groupId = "asset-radar-analysis",
            containerFactory = "assetPriceKafkaListenerContainerFactory"
    )
    public void consume(AssetPrice price) {
        AssetPrice previousPrice = latestPrices.put(keyOf(price), price);
        AssetAnalysis analysis = assetAnalysisCalculator.analyze(previousPrice, price);

        for (AssetAnalysisSink sink : sinks) {
            try {
                sink.persist(analysis).onErrorResume(error -> {
                    log.error(
                            "Analysis sink {} failed for {}:{}:{}",
                            sink.sinkName(),
                            analysis.source(),
                            analysis.quoteCurrency(),
                            analysis.symbol(),
                            error
                    );
                    return Mono.empty();
                }).block();
            } catch (RuntimeException exception) {
                log.error(
                        "Analysis sink {} threw unexpectedly for {}:{}:{}",
                        sink.sinkName(),
                        analysis.source(),
                        analysis.quoteCurrency(),
                        analysis.symbol(),
                        exception
                );
            }
        }
    }

    private String keyOf(AssetPrice price) {
        return "%s:%s:%s".formatted(price.source(), price.quoteCurrency(), price.symbol());
    }
}
