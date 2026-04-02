package com.jean202.assetradar.infra;

import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceSink;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaAssetPricePublisher implements AssetPriceSink {
    private final KafkaTemplate<String, AssetPrice> kafkaTemplate;
    private final PipelineProperties pipelineProperties;

    public KafkaAssetPricePublisher(
            KafkaTemplate<String, AssetPrice> kafkaTemplate,
            PipelineProperties pipelineProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Mono<Void> persist(AssetPrice price) {
        return Mono.defer(() -> Mono.fromFuture(
                        kafkaTemplate.send(pipelineProperties.getKafkaTopic(), keyOf(price), price)))
                .then();
    }

    @Override
    public String sinkName() {
        return "kafka";
    }

    private String keyOf(AssetPrice price) {
        return "%s:%s:%s".formatted(price.source(), price.quoteCurrency(), price.symbol());
    }
}
