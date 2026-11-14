package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.query.AssetAlertHistoryQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AssetAlertHistoryRepository(쓰기)와 PostgresAssetAlertHistoryReader(읽기)를
 * 실제 스키마가 올라간 Postgres에 붙여 SQL 문법과 컬럼 매핑을 검증한다.
 */
class PostgresAssetAlertHistoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private AssetAlertHistoryRepository repository;
    private PostgresAssetAlertHistoryReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetAlertHistoryRepository(databaseClient);
        reader = new PostgresAssetAlertHistoryReader(databaseClient);
    }

    @Test
    void persistedAlertIsReadBackWithAllColumnsMapped() {
        AssetAlert alert = new AssetAlert(
                "BTC",
                "KRW",
                "UPBIT",
                "PRICE_CHANGE",
                "WARN",
                "UP",
                new BigDecimal("137500000.00000000"),
                new BigDecimal("134500000.00000000"),
                new BigDecimal("3000000.00000000"),
                new BigDecimal("0.02230483"),
                new BigDecimal("0.01000000"),
                Instant.parse("2026-05-17T10:14:00Z"),
                60L,
                "BTC moved 2.23% in 60 seconds",
                Instant.parse("2026-05-17T10:15:03Z")
        );

        repository.persist(alert).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery("BTC", "UPBIT", "KRW", List.of(), null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        AssetAlert row = found.get(0);
        assertThat(row.symbol()).isEqualTo("BTC");
        assertThat(row.quoteCurrency()).isEqualTo("KRW");
        assertThat(row.source()).isEqualTo("UPBIT");
        assertThat(row.alertType()).isEqualTo("PRICE_CHANGE");
        assertThat(row.severity()).isEqualTo("WARN");
        assertThat(row.movement()).isEqualTo("UP");
        assertThat(row.currentPrice()).isEqualByComparingTo("137500000");
        assertThat(row.previousPrice()).isEqualByComparingTo("134500000");
        assertThat(row.priceChange()).isEqualByComparingTo("3000000");
        assertThat(row.changeRate()).isEqualByComparingTo("0.02230483");
        assertThat(row.thresholdRate()).isEqualByComparingTo("0.01");
        assertThat(row.baselineAt()).isEqualTo(Instant.parse("2026-05-17T10:14:00Z"));
        assertThat(row.windowSeconds()).isEqualTo(60L);
        assertThat(row.message()).isEqualTo("BTC moved 2.23% in 60 seconds");
        assertThat(row.alertedAt()).isEqualTo(Instant.parse("2026-05-17T10:15:03Z"));
    }

    @Test
    void readHistoryReturnsEmptyWhenNoRowMatches() {
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery("ETH", null, null, List.of(), null, null, 10))
                .collectList()
                .block();

        assertThat(found).isEmpty();
    }

    /** symbol이 null이면 WHERE 1 = 1만 남아 전체 조회가 되어야 한다. */
    @Test
    void readHistoryWithoutSymbolReturnsAllAlerts() {
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(sampleAlert("ETH", "UPBIT", "KRW", "INFO", Instant.parse("2026-05-17T10:15:04Z"))).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery(null, null, null, List.of(), null, null, 10))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAlert::symbol).containsExactlyInAnyOrder("BTC", "ETH");
    }

    /** severity IN (...) 절은 바인딩 이름을 동적으로 만들어 붙이므로 다중 값에서 특히 깨지기 쉽다. */
    @Test
    void readHistoryFiltersByMultipleSeverities() {
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(sampleAlert("ETH", "UPBIT", "KRW", "INFO", Instant.parse("2026-05-17T10:15:04Z"))).block();
        repository.persist(sampleAlert("XRP", "UPBIT", "KRW", "CRITICAL", Instant.parse("2026-05-17T10:15:05Z"))).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery(
                        null, null, null, List.of("WARN", "CRITICAL"), null, null, 10))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAlert::symbol).containsExactly("XRP", "BTC");
    }

    @Test
    void readHistoryOrdersByAlertedAtDescendingAndAppliesLimit() {
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:01Z"))).block();
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:15:02Z"))).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery("BTC", "UPBIT", "KRW", List.of(), null, null, 2))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAlert::alertedAt).containsExactly(
                Instant.parse("2026-05-17T10:15:03Z"),
                Instant.parse("2026-05-17T10:15:02Z"));
    }

    /** from/to는 SQL에서 >= 와 <= 라 경계값이 포함되어야 한다. */
    @Test
    void readHistoryWindowBoundsAreInclusive() {
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T09:59:59Z"))).block();
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T10:00:00Z"))).block();
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T11:00:00Z"))).block();
        repository.persist(sampleAlert("BTC", "UPBIT", "KRW", "WARN", Instant.parse("2026-05-17T11:00:01Z"))).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        List.of(),
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z"),
                        10))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAlert::alertedAt).containsExactly(
                Instant.parse("2026-05-17T11:00:00Z"),
                Instant.parse("2026-05-17T10:00:00Z"));
    }

    /** previous_price는 NULL 허용 컬럼이라 기준 가격이 없는 알림도 그대로 왕복되어야 한다. */
    @Test
    void persistAndReadBackNullPreviousPrice() {
        repository.persist(new AssetAlert(
                "BTC",
                "KRW",
                "UPBIT",
                "PRICE_CHANGE",
                "INFO",
                "FLAT",
                new BigDecimal("137500000"),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.01"),
                Instant.parse("2026-05-17T10:14:00Z"),
                60L,
                "no baseline",
                Instant.parse("2026-05-17T10:15:03Z")
        )).block();

        List<AssetAlert> found = reader
                .readHistory(new AssetAlertHistoryQuery("BTC", null, null, List.of(), null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).previousPrice()).isNull();
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
