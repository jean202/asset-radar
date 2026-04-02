package com.jean202.assetradar.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * E2E 통합 테스트: docker-compose로 Kafka/Redis/PostgreSQL을 띄운 뒤 실행.
 *
 * 실행 방법:
 *   docker compose up -d
 *   INTEGRATION_TEST=true ./gradlew test --tests "*PipelineIntegrationTest"
 *   docker compose down
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "asset-radar.coin.enabled=false",
                "asset-radar.gold.enabled=false",
                "asset-radar.stock-kr.enabled=false",
                "asset-radar.stock-us.enabled=false",
                "asset-radar.alert.notifier.slack.enabled=false",
                "asset-radar.alert.notifier.webhook.enabled=false"
        }
)
@AutoConfigureWebTestClient
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TEST", matches = "true")
class PipelineIntegrationTest {

    @Autowired
    private KafkaTemplate<String, AssetPrice> kafkaTemplate;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private PipelineProperties pipelineProperties;

    @Autowired
    private AssetPriceStore assetPriceStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void kafkaPublishAndConsumeWorks() throws Exception {
        AssetPrice testPrice = new AssetPrice(
                "BTC", "KRW", "UPBIT",
                new BigDecimal("137500000"),
                new BigDecimal("0.023"),
                Instant.now()
        );

        kafkaTemplate.send(pipelineProperties.getKafkaTopic(), "UPBIT:KRW:BTC", testPrice)
                .get(10, TimeUnit.SECONDS);

        try (KafkaConsumer<String, String> consumer = createTestConsumer()) {
            consumer.subscribe(List.of(pipelineProperties.getKafkaTopic()));
            List<String> received = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10_000;
            while (received.isEmpty() && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> received.add(record.value()));
            }
            assertThat(received).isNotEmpty();
            assertThat(received.get(0)).contains("BTC");
        }
    }

    @Test
    void redisAndPostgresSinksWork() throws Exception {
        AssetPrice testPrice = new AssetPrice(
                "XAU", "USD", "GOLDAPI",
                new BigDecimal("4700.00"),
                new BigDecimal("0.005"),
                Instant.now()
        );

        // In-memory store
        assetPriceStore.update(testPrice);
        assertThat(assetPriceStore.snapshot())
                .anyMatch(p -> "XAU".equals(p.symbol()) && "GOLDAPI".equals(p.source()));

        // Redis
        String redisKey = "%s:GOLDAPI:USD:XAU".formatted(pipelineProperties.getRedisKeyPrefix());
        String payload = objectMapper.writeValueAsString(testPrice);
        redisTemplate.opsForValue()
                .set(redisKey, payload, pipelineProperties.getRedisTtl())
                .block(Duration.ofSeconds(5));

        String cached = redisTemplate.opsForValue().get(redisKey).block(Duration.ofSeconds(5));
        assertThat(cached).isNotNull().contains("XAU").contains("4700");

        // PostgreSQL
        databaseClient.sql("""
                        INSERT INTO asset_price_history (
                            symbol, quote_currency, source, price, signed_change_rate, collected_at
                        ) VALUES (:symbol, :quoteCurrency, :source, :price, :signedChangeRate, :collectedAt)
                        """)
                .bind("symbol", testPrice.symbol())
                .bind("quoteCurrency", testPrice.quoteCurrency())
                .bind("source", testPrice.source())
                .bind("price", testPrice.price())
                .bind("signedChangeRate", testPrice.signedChangeRate())
                .bind("collectedAt", testPrice.collectedAt())
                .fetch()
                .rowsUpdated()
                .block(Duration.ofSeconds(5));

        Long count = databaseClient.sql(
                        "SELECT COUNT(*) FROM asset_price_history WHERE symbol = 'XAU' AND source = 'GOLDAPI'")
                .map(row -> row.get(0, Long.class))
                .first()
                .block(Duration.ofSeconds(5));
        assertThat(count).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void dashboardApiResponds() {
        webTestClient.get().uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void healthEndpointIsUp() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    private KafkaConsumer<String, String> createTestConsumer() {
        var props = new java.util.Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-verification-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
