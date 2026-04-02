package com.jean202.assetradar.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    NewTopic assetPriceTopic(PipelineProperties pipelineProperties) {
        return TopicBuilder.name(pipelineProperties.getKafkaTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic assetAnalysisTopic(PipelineProperties pipelineProperties) {
        return TopicBuilder.name(pipelineProperties.getAnalysisKafkaTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
