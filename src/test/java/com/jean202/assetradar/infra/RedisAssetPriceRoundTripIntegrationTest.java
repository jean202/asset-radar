package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.LatestAssetQuery;
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
 * RedisAssetPriceCache(쓰기)와 RedisLatestAssetPriceReader(읽기)는 키 규약을 공유한다.
 * 두 클래스를 같은 컨테이너에 붙여 검증해야 키 포맷 불일치를 잡을 수 있다.
 */
@Testcontainers
class RedisAssetPriceRoundTripIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private ReactiveStringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private PipelineProperties pipelineProperties;
    private RedisAssetPriceCache cache;
    private RedisLatestAssetPriceReader reader;

    @BeforeEach
    void setUp() {
        if (connectionFactory == null) {
            connectionFactory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
            connectionFactory.afterPropertiesSet();
        }

        redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
        redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        pipelineProperties = new PipelineProperties();

        cache = new RedisAssetPriceCache(redisTemplate, objectMapper, pipelineProperties);
        reader = new RedisLatestAssetPriceReader(redisTemplate, objectMapper, pipelineProperties);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
            connectionFactory = null;
        }
    }

    @Test
    void persistedPriceIsReadBackByLatestReader() {
        AssetPrice price = samplePrice("BTC", "UPBIT", "KRW", new BigDecimal("137500000"));

        cache.persist(price).block();

        List<AssetPrice> found = reader
                .readLatest(new LatestAssetQuery("UPBIT", "KRW", List.of("BTC")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).symbol()).isEqualTo("BTC");
        assertThat(found.get(0).price()).isEqualByComparingTo("137500000");
        assertThat(found.get(0).collectedAt()).isEqualTo(price.collectedAt());
    }

    @Test
    void latestReaderFiltersBySourceAndQuoteCurrency() {
        cache.persist(samplePrice("BTC", "UPBIT", "KRW", new BigDecimal("137500000"))).block();
        cache.persist(samplePrice("BTC", "BINANCE", "USDT", new BigDecimal("96000"))).block();
        cache.persist(samplePrice("ETH", "UPBIT", "KRW", new BigDecimal("5100000"))).block();

        List<AssetPrice> upbitKrw = reader
                .readLatest(new LatestAssetQuery("UPBIT", "KRW", List.of()))
                .collectList()
                .block();

        assertThat(upbitKrw).extracting(AssetPrice::symbol).containsExactly("BTC", "ETH");
    }

    @Test
    void persistOverwritesPreviousPriceForSameKey() {
        cache.persist(samplePrice("BTC", "UPBIT", "KRW", new BigDecimal("137500000"))).block();
        cache.persist(samplePrice("BTC", "UPBIT", "KRW", new BigDecimal("140000000"))).block();

        List<AssetPrice> found = reader
                .readLatest(new LatestAssetQuery("UPBIT", "KRW", List.of("BTC")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).price()).isEqualByComparingTo("140000000");
    }

    @Test
    void persistAppliesConfiguredTtl() {
        AssetPrice price = samplePrice("BTC", "UPBIT", "KRW", new BigDecimal("137500000"));

        cache.persist(price).block();

        Long ttlSeconds = redisTemplate
                .getExpire("%s:UPBIT:KRW:BTC".formatted(pipelineProperties.getRedisKeyPrefix()))
                .map(java.time.Duration::toSeconds)
                .block();

        assertThat(ttlSeconds)
                .isNotNull()
                .isGreaterThan(0L)
                .isLessThanOrEqualTo(pipelineProperties.getRedisTtl().toSeconds());
    }

    private AssetPrice samplePrice(String symbol, String source, String quoteCurrency, BigDecimal price) {
        return new AssetPrice(
                symbol,
                symbol,
                quoteCurrency,
                source,
                price,
                new BigDecimal("0.018"),
                Instant.parse("2026-05-17T10:15:02Z")
        );
    }
}
