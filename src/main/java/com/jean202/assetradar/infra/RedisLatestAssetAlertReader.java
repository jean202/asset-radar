package com.jean202.assetradar.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.query.LatestAssetAlertQuery;
import com.jean202.assetradar.query.LatestAssetAlertReader;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RedisLatestAssetAlertReader implements LatestAssetAlertReader {
    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
            "CRITICAL", 0,
            "WARN", 1,
            "INFO", 2
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties pipelineProperties;

    public RedisLatestAssetAlertReader(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties pipelineProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pipelineProperties = pipelineProperties;
    }

    @Override
    public Flux<AssetAlert> readLatest(LatestAssetAlertQuery query) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(query.redisKeyPattern(pipelineProperties.getAlertRedisKeyPrefix()))
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
                        .comparingInt((AssetAlert alert) -> SEVERITY_ORDER.getOrDefault(alert.severity(), 99))
                        .thenComparing(AssetAlert::alertedAt, Comparator.reverseOrder())
                        .thenComparing(AssetAlert::source)
                        .thenComparing(AssetAlert::quoteCurrency)
                        .thenComparing(AssetAlert::symbol));
    }

    private Mono<AssetAlert> deserialize(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, AssetAlert.class));
    }
}
