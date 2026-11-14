package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.config.PipelineProperties;
import com.jean202.assetradar.domain.AssetAnalysis;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * KafkaAssetAnalysisPublisher가 실제 브로커에 발행한 레코드의 토픽/키/값을 검증한다.
 * 값은 프로덕션 consumer 설정과 같은 방식으로 역직렬화해 왕복까지 확인한다.
 */
class KafkaAssetAnalysisPublisherIntegrationTest extends AbstractKafkaIntegrationTest {

    private static final AtomicInteger TOPIC_SEQUENCE = new AtomicInteger();

    private String topic;
    private KafkaAssetAnalysisPublisher publisher;

    @BeforeEach
    void setUpPublisher() {
        topic = "test.asset.analysis." + TOPIC_SEQUENCE.incrementAndGet();
        createTopic(topic);

        PipelineProperties pipelineProperties = new PipelineProperties();
        pipelineProperties.setAnalysisKafkaTopic(topic);

        KafkaTemplate<String, AssetAnalysis> kafkaTemplate = kafkaTemplate();
        publisher = new KafkaAssetAnalysisPublisher(kafkaTemplate, pipelineProperties);
    }

    /** 가격 토픽이 아니라 analysisKafkaTopic으로 나가야 한다. */
    @Test
    void publishesToConfiguredAnalysisTopicWithSourceQuoteSymbolKey() {
        publisher.persist(sampleAnalysis("BTC", "UPBIT", "KRW", "137500000", "UP")).block();

        List<ConsumerRecord<String, AssetAnalysis>> records = consume(topic, AssetAnalysis.class, 1);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).topic()).isEqualTo(topic);
        assertThat(records.get(0).key()).isEqualTo("UPBIT:KRW:BTC");
    }

    @Test
    void publishedValueDeserializesBackToAssetAnalysis() {
        AssetAnalysis analysis = sampleAnalysis("BTC", "UPBIT", "KRW", "137500000", "UP");

        publisher.persist(analysis).block();

        List<ConsumerRecord<String, AssetAnalysis>> records = consume(topic, AssetAnalysis.class, 1);

        assertThat(records).hasSize(1);
        AssetAnalysis received = records.get(0).value();
        assertThat(received.symbol()).isEqualTo("BTC");
        assertThat(received.quoteCurrency()).isEqualTo("KRW");
        assertThat(received.source()).isEqualTo("UPBIT");
        assertThat(received.currentPrice()).isEqualByComparingTo("137500000");
        assertThat(received.previousPrice()).isEqualByComparingTo("136900000");
        assertThat(received.priceChange()).isEqualByComparingTo("600000");
        assertThat(received.changeRate()).isEqualByComparingTo("0.00438276");
        assertThat(received.movement()).isEqualTo("UP");
        assertThat(received.analyzedAt()).isEqualTo(analysis.analyzedAt());
    }

    /** previousPrice가 null인 첫 분석 결과도 JSON 왕복에서 살아남아야 한다. */
    @Test
    void publishedValueKeepsNullPreviousPrice() {
        publisher.persist(new AssetAnalysis(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000"),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "FLAT",
                Instant.parse("2026-05-17T10:15:03Z")
        )).block();

        List<ConsumerRecord<String, AssetAnalysis>> records = consume(topic, AssetAnalysis.class, 1);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).value().previousPrice()).isNull();
        assertThat(records.get(0).value().movement()).isEqualTo("FLAT");
    }

    @Test
    void publishesDistinctKeyPerAsset() {
        publisher.persist(sampleAnalysis("BTC", "UPBIT", "KRW", "137500000", "UP")).block();
        publisher.persist(sampleAnalysis("ETH", "UPBIT", "KRW", "5100000", "DOWN")).block();
        publisher.persist(sampleAnalysis("BTC", "BINANCE", "USDT", "96000", "UP")).block();

        List<ConsumerRecord<String, AssetAnalysis>> records = consume(topic, AssetAnalysis.class, 3);

        assertThat(records).extracting(ConsumerRecord::key)
                .containsExactlyInAnyOrder("UPBIT:KRW:BTC", "UPBIT:KRW:ETH", "BINANCE:USDT:BTC");
    }

    @Test
    void sinkNameIsAnalysisKafka() {
        assertThat(publisher.sinkName()).isEqualTo("analysis-kafka");
    }

    private AssetAnalysis sampleAnalysis(
            String symbol,
            String source,
            String quoteCurrency,
            String currentPrice,
            String movement
    ) {
        return new AssetAnalysis(
                symbol,
                quoteCurrency,
                source,
                new BigDecimal(currentPrice),
                new BigDecimal("136900000"),
                new BigDecimal("600000"),
                new BigDecimal("0.00438276"),
                movement,
                Instant.parse("2026-05-17T10:15:03Z")
        );
    }
}
