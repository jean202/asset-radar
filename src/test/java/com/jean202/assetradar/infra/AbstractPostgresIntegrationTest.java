package com.jean202.assetradar.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres 리더/리포지토리 통합 테스트의 공통 베이스.
 *
 * Postgres를 쓰는 테스트 클래스가 여러 개라 @Container(클래스 단위 start/stop)를 쓰면
 * 클래스마다 컨테이너를 새로 띄우게 된다. Testcontainers 싱글턴 패턴으로 JVM당 한 번만 띄우고,
 * 정리는 Ryuk에 맡긴다. 테스트 간 격리는 재기동이 아니라 TRUNCATE로 한다.
 */
abstract class AbstractPostgresIntegrationTest {

    private static final List<String> TABLES = List.of(
            "asset_price_history",
            "asset_analysis_history",
            "asset_alert_history"
    );

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("asset_radar")
                    .withUsername("asset_radar")
                    .withPassword("asset_radar");

    private static DatabaseClient sharedClient;

    static {
        POSTGRES.start();
        sharedClient = DatabaseClient.create(io.r2dbc.spi.ConnectionFactories.get(
                "r2dbc:postgresql://%s:%s@%s:%d/%s".formatted(
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword(),
                        POSTGRES.getHost(),
                        POSTGRES.getFirstMappedPort(),
                        POSTGRES.getDatabaseName())));
        applySchema(sharedClient);
    }

    protected final DatabaseClient databaseClient = sharedClient;

    @BeforeEach
    void truncateTables() {
        databaseClient.sql("TRUNCATE TABLE %s RESTART IDENTITY".formatted(String.join(", ", TABLES)))
                .fetch()
                .rowsUpdated()
                .block(Duration.ofSeconds(10));
    }

    /** 프로덕션 schema.sql을 그대로 적용해야 실제 컬럼 타입/제약과 SQL이 맞는지 검증된다. */
    private static void applySchema(DatabaseClient client) {
        String schema;
        try (InputStream in = AbstractPostgresIntegrationTest.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on test classpath");
            }
            schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read schema.sql", e);
        }

        Arrays.stream(schema.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isEmpty())
                .forEach(statement -> client.sql(statement)
                        .fetch()
                        .rowsUpdated()
                        .block(Duration.ofSeconds(20)));
    }
}
