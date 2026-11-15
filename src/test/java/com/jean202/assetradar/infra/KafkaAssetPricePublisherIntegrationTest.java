package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * KafkaAssetPricePublisher가 실제 브로커에 발행한 레코드의 토픽/키/값을 검증한다.
 * 값은 프로덕션 consumer 설정과 같은 방식으로 역직렬화해 왕복까지 확인한다.
 */
class KafkaAssetPricePublisherIntegrationTest extends AbstractKafkaIntegrationTest {

    private static final AtomicInteger TOPIC_SEQUENCE = new AtomicInteger();

    private String topic;
    private PipelineProperties pipelineProperties;
    private KafkaAssetPricePublisher publisher;

    @BeforeEach
    void setUpPublisher() {
        topic = "test.asset.price." + TOPIC_SEQUENCE.incrementAndGet();
        createTopic(topic);

        pipelineProperties = new PipelineProperties();
        pipelineProperties.setKafkaTopic(topic);

        KafkaTemplate<String, AssetPrice> kafkaTemplate = kafkaTemplate();
        publisher = new KafkaAssetPricePublisher(kafkaTemplate, pipelineProperties);
    }

    @Test
    void publishesToConfiguredTopicWithSourceQuoteSymbolKey() {
        publisher.persist(samplePrice("BTC", "UPBIT", "KRW", "137500000")).block();

        List<ConsumerRecord<String, AssetPrice>> records = consume(topic, AssetPrice.class, 1);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).topic()).isEqualTo(topic);
        assertThat(records.get(0).key()).isEqualTo("UPBIT:KRW:BTC");
    }

    @Test
    void publishedValueDeserializesBackToAssetPrice() {
        AssetPrice price = samplePrice("BTC", "UPBIT", "KRW", "137500000");

        publisher.persist(price).block();

        List<ConsumerRecord<String, AssetPrice>> records = consume(topic, AssetPrice.class, 1);

        assertThat(records).hasSize(1);
        AssetPrice received = records.get(0).value();
        assertThat(received.symbol()).isEqualTo("BTC");
        assertThat(received.name()).isEqualTo("비트코인");
        assertThat(received.quoteCurrency()).isEqualTo("KRW");
        assertThat(received.source()).isEqualTo("UPBIT");
        assertThat(received.price()).isEqualByComparingTo("137500000");
        assertThat(received.signedChangeRate()).isEqualByComparingTo("0.018");
        assertThat(received.collectedAt()).isEqualTo(price.collectedAt());
    }

    /** 키가 source:quote:symbol이라 자산별로 달라야 파티셔닝이 의미를 갖는다. */
    @Test
    void publishesDistinctKeyPerAsset() {
        publisher.persist(samplePrice("BTC", "UPBIT", "KRW", "137500000")).block();
        publisher.persist(samplePrice("ETH", "UPBIT", "KRW", "5100000")).block();
        publisher.persist(samplePrice("BTC", "BINANCE", "USDT", "96000")).block();

        List<ConsumerRecord<String, AssetPrice>> records = consume(topic, AssetPrice.class, 3);

        assertThat(records).extracting(ConsumerRecord::key)
                .containsExactlyInAnyOrder("UPBIT:KRW:BTC", "UPBIT:KRW:ETH", "BINANCE:USDT:BTC");
    }

    @Test
    void sinkNameIsKafka() {
        assertThat(publisher.sinkName()).isEqualTo("kafka");
    }

    private AssetPrice samplePrice(String symbol, String source, String quoteCurrency, String price) {
        return new AssetPrice(
                symbol,
                "BTC".equals(symbol) ? "비트코인" : symbol,
                quoteCurrency,
                source,
                new BigDecimal(price),
                new BigDecimal("0.018"),
                Instant.parse("2026-05-17T10:15:02Z")
        );
    }
}
