package com.jean202.assetradar.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RedisLatestAssetAnalysisReader implements LatestAssetAnalysisReader {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties pipelineProperties;

    public RedisLatestAssetAnalysisReader(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties pipelineProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Flux<AssetAnalysis> readLatest(LatestAssetAnalysisQuery query) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(query.redisKeyPattern(pipelineProperties.getAnalysisRedisKeyPrefix()))
                .count(100)
                .build();

        return redisTemplate.scan(scanOptions)
                .collectList()
                .flatMapMany(keys -> {
                    if (keys.isEmpty()) {
                        return Flux.empty();
                    }

                    return redisTemplate.opsForValue()
                            .multiGet(keys)
                            .flatMapMany(values -> Flux.fromIterable(values == null ? java.util.List.<String>of() : values));
                })
                .filter(Objects::nonNull)
                .flatMap(this::deserialize)
                .filter(query::matches)
                .sort(Comparator
                        .comparing(AssetAnalysis::source)
                        .thenComparing(AssetAnalysis::quoteCurrency)
                        .thenComparing(AssetAnalysis::symbol));
    }

    private Mono<AssetAnalysis> deserialize(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, AssetAnalysis.class));
    }
}
