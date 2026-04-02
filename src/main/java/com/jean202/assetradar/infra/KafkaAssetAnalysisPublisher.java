package com.jean202.assetradar.infra;

import com.jean202.assetradar.analysis.AssetAnalysisSink;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAnalysis;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaAssetAnalysisPublisher implements AssetAnalysisSink {
    private final KafkaTemplate<String, AssetAnalysis> kafkaTemplate;
    private final PipelineProperties pipelineProperties;

    public KafkaAssetAnalysisPublisher(
            KafkaTemplate<String, AssetAnalysis> kafkaTemplate,
            PipelineProperties pipelineProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Mono<Void> persist(AssetAnalysis analysis) {
        return Mono.defer(() -> Mono.fromFuture(
                        kafkaTemplate.send(pipelineProperties.getAnalysisKafkaTopic(), keyOf(analysis), analysis)))
                .then();
    }

    @Override
    public String sinkName() {
        return "analysis-kafka";
    }

    private String keyOf(AssetAnalysis analysis) {
        return "%s:%s:%s".formatted(analysis.source(), analysis.quoteCurrency(), analysis.symbol());
    }
}
