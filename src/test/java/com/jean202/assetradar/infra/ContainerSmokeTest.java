package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 야간 통합 테스트 작업이 의존하는 컨테이너 3종 중 Kafka/Postgres가
 * 이 머신에서 실제로 기동되고 접속되는지 확인하는 스모크 테스트.
 */
@Testcontainers
class ContainerSmokeTest {

    @Container
    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.1.2"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("asset_radar")
                    .withUsername("asset_radar")
                    .withPassword("asset_radar");

    @Test
    void kafkaContainerAcceptsProduceAndConsume() throws Exception {
        String topic = "smoke.asset.price";

        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(adminProps)) {
            admin.createTopics(java.util.List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        }

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, "UPBIT:KRW:BTC", "{\"symbol\":\"BTC\"}")).get();
        }

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "smoke-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);
            assertThat(records.iterator().next().key()).isEqualTo("UPBIT:KRW:BTC");
        }
    }

    @Test
    void postgresContainerAcceptsR2dbcQuery() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(
                "r2dbc:postgresql://%s:%s@%s:%d/%s".formatted(
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword(),
                        POSTGRES.getHost(),
                        POSTGRES.getFirstMappedPort(),
                        POSTGRES.getDatabaseName()));

        DatabaseClient client = DatabaseClient.create(connectionFactory);

        client.sql("CREATE TABLE smoke_price (symbol VARCHAR(16) PRIMARY KEY, price NUMERIC)")
                .fetch()
                .rowsUpdated()
                .block(Duration.ofSeconds(10));

        client.sql("INSERT INTO smoke_price (symbol, price) VALUES ('BTC', 137500000)")
                .fetch()
                .rowsUpdated()
                .block(Duration.ofSeconds(10));

        Map<String, Object> row = client.sql("SELECT symbol, price FROM smoke_price")
                .fetch()
                .one()
                .block(Duration.ofSeconds(10));

        assertThat(row).isNotNull();
        assertThat(row.get("symbol")).isEqualTo("BTC");
    }
}
