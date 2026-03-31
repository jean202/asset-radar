package com.jean202.assetradar.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.alert.AssetAlertSink;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAlert;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisAssetAlertCache implements AssetAlertSink {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties pipelineProperties;

    public RedisAssetAlertCache(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties pipelineProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Mono<Void> persist(AssetAlert alert) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(alert))
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(keyOf(alert), payload, pipelineProperties.getRedisTtl()))
                .then();
    }

    @Override
    public String sinkName() {
        return "alert-redis";
    }

    private String keyOf(AssetAlert alert) {
        return "%s:%s:%s:%s".formatted(
                pipelineProperties.getAlertRedisKeyPrefix(),
                alert.source(),
                alert.quoteCurrency(),
                alert.symbol()
        );
    }
}
