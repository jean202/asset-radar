package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.query.LatestAssetAlertQuery;
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
 * RedisAssetAlertCache(쓰기)와 RedisLatestAssetAlertReader(읽기)는 키 규약을 공유한다.
 * 두 클래스를 같은 컨테이너에 붙여 검증해야 키 포맷 불일치를 잡을 수 있다.
 */
@Testcontainers
class RedisAssetAlertRoundTripIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private ReactiveStringRedisTemplate redisTemplate;
    private PipelineProperties pipelineProperties;
    private RedisAssetAlertCache cache;
    private RedisLatestAssetAlertReader reader;

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

        cache = new RedisAssetAlertCache(redisTemplate, objectMapper, pipelineProperties);
        reader = new RedisLatestAssetAlertReader(redisTemplate, objectMapper, pipelineProperties);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
            connectionFactory = null;
        }
    }

    @Test
    void persistedAlertIsReadBackByLatestReader() {
        AssetAlert alert = sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"));

        cache.persist(alert).block();

        List<AssetAlert> found = reader
                .readLatest(new LatestAssetAlertQuery("UPBIT", "KRW", List.of("BTC"), List.of()))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).symbol()).isEqualTo("BTC");
        assertThat(found.get(0).severity()).isEqualTo("WARN");
        assertThat(found.get(0).alertType()).isEqualTo("PRICE_CHANGE");
        assertThat(found.get(0).currentPrice()).isEqualByComparingTo("137500000");
        assertThat(found.get(0).windowSeconds()).isEqualTo(60L);
        assertThat(found.get(0).baselineAt()).isEqualTo(alert.baselineAt());
        assertThat(found.get(0).alertedAt()).isEqualTo(alert.alertedAt());
    }

    @Test
    void latestReaderFiltersBySourceQuoteCurrencyAndSeverity() {
        cache.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();
        cache.persist(sampleAlert("ETH", "UPBIT", "KRW", "INFO", Instant.parse("2026-05-17T10:15:04Z"))).block();
        cache.persist(sampleAlert("BTC", "BINANCE", "USDT", "WARN", Instant.parse("2026-05-17T10:15:05Z"))).block();

        List<AssetAlert> upbitKrwWarn = reader
                .readLatest(new LatestAssetAlertQuery("UPBIT", "KRW", List.of(), List.of("WARN")))
                .collectList()
                .block();

        assertThat(upbitKrwWarn).extracting(AssetAlert::symbol).containsExactly("BTC");
    }

    @Test
    void latestReaderSortsBySeverityThenNewestFirst() {
        cache.persist(sampleAlert("ETH", "UPBIT", "KRW", "INFO", Instant.parse("2026-05-17T10:15:09Z"))).block();
        cache.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();
        cache.persist(sampleAlert("XRP", "UPBIT", "KRW", "CRITICAL", Instant.parse("2026-05-17T10:15:01Z"))).block();
        cache.persist(sampleAlert("SOL", "UPBIT", "KRW", "CRITICAL", Instant.parse("2026-05-17T10:15:07Z"))).block();

        List<AssetAlert> found = reader
                .readLatest(new LatestAssetAlertQuery("UPBIT", "KRW", List.of(), List.of()))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAlert::symbol).containsExactly("SOL", "XRP", "BTC", "ETH");
    }

    @Test
    void persistOverwritesPreviousAlertForSameKey() {
        cache.persist(sampleAlert("BTC", "UPBIT", "KRW", "INFO", Instant.parse("2026-05-17T10:15:03Z"))).block();
        cache.persist(sampleAlert("BTC", "UPBIT", "KRW", "CRITICAL", Instant.parse("2026-05-17T10:16:03Z"))).block();

        List<AssetAlert> found = reader
                .readLatest(new LatestAssetAlertQuery("UPBIT", "KRW", List.of("BTC"), List.of()))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).severity()).isEqualTo("CRITICAL");
        assertThat(found.get(0).alertedAt()).isEqualTo(Instant.parse("2026-05-17T10:16:03Z"));
    }

    @Test
    void persistAppliesConfiguredTtl() {
        cache.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();

        Long ttlSeconds = redisTemplate
                .getExpire("%s:UPBIT:KRW:BTC".formatted(pipelineProperties.getAlertRedisKeyPrefix()))
                .map(java.time.Duration::toSeconds)
                .block();

        assertThat(ttlSeconds)
                .isNotNull()
                .isGreaterThan(0L)
                .isLessThanOrEqualTo(pipelineProperties.getRedisTtl().toSeconds());
    }

    private AssetAlert sampleAlert(
            String symbol,
            String source,
            String quoteCurrency,
            String severity,
            Instant alertedAt
    ) {
        return new AssetAlert(
                symbol,
                quoteCurrency,
                source,
                "PRICE_CHANGE",
                severity,
                "UP",
                new BigDecimal("137500000"),
                new BigDecimal("134500000"),
                new BigDecimal("3000000"),
                new BigDecimal("0.02230483"),
                new BigDecimal("0.01"),
                Instant.parse("2026-05-17T10:14:00Z"),
                60L,
                "%s moved 2.23%% in 60 seconds".formatted(symbol),
                alertedAt
        );
    }
}
