package com.jean202.assetradar.config;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetPrice;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Bean
    ConsumerFactory<String, AssetPrice> assetPriceConsumerFactory(KafkaProperties kafkaProperties) {
        return consumerFactory(kafkaProperties, AssetPrice.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, AssetPrice> assetPriceKafkaListenerContainerFactory(
            ConsumerFactory<String, AssetPrice> assetPriceConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AssetPrice> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(assetPriceConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, AssetAnalysis> assetAnalysisConsumerFactory(KafkaProperties kafkaProperties) {
        return consumerFactory(kafkaProperties, AssetAnalysis.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, AssetAnalysis> assetAnalysisKafkaListenerContainerFactory(
            ConsumerFactory<String, AssetAnalysis> assetAnalysisConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, AssetAnalysis> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(assetAnalysisConsumerFactory);
        return factory;
    }

    private <T> ConsumerFactory<String, T> consumerFactory(KafkaProperties kafkaProperties, Class<T> valueType) {
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildConsumerProperties());
        properties.remove("spring.json.value.default.type");
        properties.remove("spring.json.trusted.packages");
        properties.remove("spring.json.use.type.headers");
        properties.remove("spring.json.remove.type.headers");
        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(valueType);
        valueDeserializer.addTrustedPackages("com.jean202.assetradar.domain");
        valueDeserializer.setUseTypeHeaders(false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), valueDeserializer);
    }
}
