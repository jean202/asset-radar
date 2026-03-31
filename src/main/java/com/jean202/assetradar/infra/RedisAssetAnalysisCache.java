package com.jean202.assetradar.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.analysis.AssetAnalysisSink;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAnalysis;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisAssetAnalysisCache implements AssetAnalysisSink {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties pipelineProperties;

    public RedisAssetAnalysisCache(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties pipelineProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Mono<Void> persist(AssetAnalysis analysis) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(analysis))
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(keyOf(analysis), payload, pipelineProperties.getRedisTtl()))
                .then();
    }

    @Override
    public String sinkName() {
        return "analysis-redis";
    }

    private String keyOf(AssetAnalysis analysis) {
        return "%s:%s:%s:%s".formatted(
                pipelineProperties.getAnalysisRedisKeyPrefix(),
                analysis.source(),
                analysis.quoteCurrency(),
                analysis.symbol()
        );
    }
}
