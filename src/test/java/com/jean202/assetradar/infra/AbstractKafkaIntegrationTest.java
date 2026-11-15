package com.jean202.assetradar.infra;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Kafka 퍼블리셔 통합 테스트의 공통 베이스.
 *
 * 브로커 기동이 가장 비싼 단계라 Testcontainers 싱글턴 패턴으로 JVM당 한 번만 띄운다.
 * 테스트 간 격리는 브로커 재기동이 아니라 테스트마다 새 토픽/새 컨슈머 그룹으로 확보한다.
 */
abstract class AbstractKafkaIntegrationTest {

    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.1.2"));

    static {
        KAFKA.start();
    }

    protected static void createTopic(String topic) {
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(adminProps)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("topic creation interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create topic " + topic, e);
        }
    }

    /** 프로덕션 producer 설정(application.yml)과 동일하게 타입 헤더 없는 JSON으로 직렬화한다. */
    protected static <T> KafkaTemplate<String, T> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    /**
     * 프로덕션 consumer 설정(KafkaConsumerConfig)과 동일하게 타입 헤더 없이 목표 타입으로 역직렬화한다.
     * 발행된 JSON이 실제로 도메인 record로 되돌아오는지가 이 테스트들의 핵심이다.
     */
    protected static <T> List<ConsumerRecord<String, T>> consume(String topic, Class<T> valueType, int expected) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, topic + "-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(valueType);
        valueDeserializer.addTrustedPackages("com.jean202.assetradar.domain");
        valueDeserializer.setUseTypeHeaders(false);

        List<ConsumerRecord<String, T>> collected = new ArrayList<>();
        try (KafkaConsumer<String, T> consumer =
                     new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer)) {
            consumer.subscribe(List.of(topic));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
            while (collected.size() < expected && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, T> records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(collected::add);
            }
        }
        return collected;
    }
}
