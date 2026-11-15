package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 Redis 컨테이너에 쓰고 읽는 왕복 검증.
 *
 * RedisAssetAnalysisCache(쓰기)와 RedisLatestAssetAnalysisReader(읽기)는 키 규약을 공유한다.
 * 두 클래스를 같은 컨테이너에 붙여 검증해야 키 포맷 불일치를 잡을 수 있다.
 */
@Testcontainers
class RedisAssetAnalysisRoundTripIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private ReactiveStringRedisTemplate redisTemplate;
    private PipelineProperties pipelineProperties;
    private RedisAssetAnalysisCache cache;
    private RedisLatestAssetAnalysisReader reader;

    @BeforeEach
    void setUp() {
        if (connectionFactory == null) {
            connectionFactory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
            connectionFactory.afterPropertiesSet();
        }

        redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
        redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        pipelineProperties = new PipelineProperties();

        cache = new RedisAssetAnalysisCache(redisTemplate, objectMapper, pipelineProperties);
        reader = new RedisLatestAssetAnalysisReader(redisTemplate, objectMapper, pipelineProperties);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
            connectionFactory = null;
        }
    }

    @Test
    void persistedAnalysisIsReadBackByLatestReader() {
        AssetAnalysis analysis = sampleAnalysis("BTC", "UPBIT", "KRW", new BigDecimal("137500000"), "UP");

        cache.persist(analysis).block();

        List<AssetAnalysis> found = reader
                .readLatest(new LatestAssetAnalysisQuery("UPBIT", "KRW", List.of("BTC")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).symbol()).isEqualTo("BTC");
        assertThat(found.get(0).currentPrice()).isEqualByComparingTo("137500000");
        assertThat(found.get(0).previousPrice()).isEqualByComparingTo("136900000");
        assertThat(found.get(0).movement()).isEqualTo("UP");
        assertThat(found.get(0).analyzedAt()).isEqualTo(analysis.analyzedAt());
    }

    @Test
    void latestReaderFiltersBySourceAndQuoteCurrency() {
        cache.persist(sampleAnalysis("BTC", "UPBIT", "KRW", new BigDecimal("137500000"), "UP")).block();
        cache.persist(sampleAnalysis("BTC", "BINANCE", "USDT", new BigDecimal("96000"), "DOWN")).block();
        cache.persist(sampleAnalysis("ETH", "UPBIT", "KRW", new BigDecimal("5100000"), "UP")).block();

        List<AssetAnalysis> upbitKrw = reader
                .readLatest(new LatestAssetAnalysisQuery("UPBIT", "KRW", List.of()))
                .collectList()
                .block();

        assertThat(upbitKrw).extracting(AssetAnalysis::symbol).containsExactly("BTC", "ETH");
    }

    @Test
    void persistOverwritesPreviousAnalysisForSameKey() {
        cache.persist(sampleAnalysis("BTC", "UPBIT", "KRW", new BigDecimal("137500000"), "UP")).block();
        cache.persist(sampleAnalysis("BTC", "UPBIT", "KRW", new BigDecimal("130000000"), "DOWN")).block();

        List<AssetAnalysis> found = reader
                .readLatest(new LatestAssetAnalysisQuery("UPBIT", "KRW", List.of("BTC")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).currentPrice()).isEqualByComparingTo("130000000");
        assertThat(found.get(0).movement()).isEqualTo("DOWN");
    }

    @Test
    void persistAppliesConfiguredTtl() {
        cache.persist(sampleAnalysis("BTC", "UPBIT", "KRW", new BigDecimal("137500000"), "UP")).block();

        Long ttlSeconds = redisTemplate
                .getExpire("%s:UPBIT:KRW:BTC".formatted(pipelineProperties.getAnalysisRedisKeyPrefix()))
                .map(java.time.Duration::toSeconds)
                .block();

        assertThat(ttlSeconds)
                .isNotNull()
                .isGreaterThan(0L)
                .isLessThanOrEqualTo(pipelineProperties.getRedisTtl().toSeconds());
    }

    private AssetAnalysis sampleAnalysis(
            String symbol,
            String source,
            String quoteCurrency,
            BigDecimal currentPrice,
            String movement
    ) {
        return new AssetAnalysis(
                symbol,
                quoteCurrency,
                source,
                currentPrice,
                new BigDecimal("136900000"),
                currentPrice.subtract(new BigDecimal("136900000")),
                new BigDecimal("0.00438276"),
                movement,
                Instant.parse("2026-05-17T10:15:03Z")
        );
    }
}
