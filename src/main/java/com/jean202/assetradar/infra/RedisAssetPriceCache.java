package com.jean202.assetradar.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceSink;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisAssetPriceCache implements AssetPriceSink {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties pipelineProperties;

    public RedisAssetPriceCache(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties pipelineProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Mono<Void> persist(AssetPrice price) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(price))
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(keyOf(price), payload, pipelineProperties.getRedisTtl()))
                .then();
    }

    @Override
    public String sinkName() {
        return "redis";
    }

    private String keyOf(AssetPrice price) {
        return "%s:%s:%s:%s".formatted(
                pipelineProperties.getRedisKeyPrefix(),
                price.source(),
                price.quoteCurrency(),
                price.symbol()
        );
    }
}
