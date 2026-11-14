package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetHistoryQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AssetPriceHistoryRepository(쓰기)와 PostgresAssetPriceHistoryReader(읽기)를
 * 실제 스키마가 올라간 Postgres에 붙여 SQL 문법과 컬럼 매핑을 검증한다.
 */
class PostgresAssetPriceHistoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private AssetPriceHistoryRepository repository;
    private PostgresAssetPriceHistoryReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetPriceHistoryRepository(databaseClient);
        reader = new PostgresAssetPriceHistoryReader(databaseClient);
    }

    @Test
    void persistedPriceIsReadBackWithAllColumnsMapped() {
        AssetPrice price = new AssetPrice(
                "BTC",
                "비트코인",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000.00000000"),
                new BigDecimal("0.01800000"),
                Instant.parse("2026-05-17T10:15:02Z")
        );

        repository.persist(price).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery("BTC", "UPBIT", "KRW", null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        AssetPrice row = found.get(0);
        assertThat(row.symbol()).isEqualTo("BTC");
        assertThat(row.name()).isEqualTo("비트코인");
        assertThat(row.quoteCurrency()).isEqualTo("KRW");
        assertThat(row.source()).isEqualTo("UPBIT");
        assertThat(row.price()).isEqualByComparingTo("137500000");
        assertThat(row.signedChangeRate()).isEqualByComparingTo("0.018");
        assertThat(row.collectedAt()).isEqualTo(Instant.parse("2026-05-17T10:15:02Z"));
    }

    @Test
    void readHistoryReturnsEmptyWhenNoRowMatches() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery("ETH", null, null, null, null, 10))
                .collectList()
                .block();

        assertThat(found).isEmpty();
    }

    @Test
    void readHistoryFiltersBySourceAndQuoteCurrency() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", Instant.parse("2026-05-17T10:15:03Z"))).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery("BTC", "BINANCE", "USDT", null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).source()).isEqualTo("BINANCE");
    }

    @Test
    void readHistoryOrdersByCollectedAtDescendingAndAppliesLimit() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:01Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery("BTC", "UPBIT", "KRW", null, null, 2))
                .collectList()
                .block();

        assertThat(found).extracting(AssetPrice::collectedAt).containsExactly(
                Instant.parse("2026-05-17T10:15:03Z"),
                Instant.parse("2026-05-17T10:15:02Z"));
    }

    /** from/to는 SQL에서 >= 와 <= 라 경계값이 포함되어야 한다. */
    @Test
    void readHistoryWindowBoundsAreInclusive() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:00:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:30:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T11:00:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T11:00:01Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T09:59:59Z"))).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z"),
                        10))
                .collectList()
                .block();

        assertThat(found).extracting(AssetPrice::collectedAt).containsExactly(
                Instant.parse("2026-05-17T11:00:00Z"),
                Instant.parse("2026-05-17T10:30:00Z"),
                Instant.parse("2026-05-17T10:00:00Z"));
    }

    /** name이 null인 가격도 NOT NULL 컬럼에 빈 문자열로 저장되어야 한다. */
    @Test
    void persistStoresEmptyNameWhenNameIsNull() {
        repository.persist(new AssetPrice(
                "BTC",
                null,
                "KRW",
                "UPBIT",
                new BigDecimal("137500000"),
                new BigDecimal("0.018"),
                Instant.parse("2026-05-17T10:15:02Z")
        )).block();

        List<AssetPrice> found = reader
                .readHistory(new AssetHistoryQuery("BTC", null, null, null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).name()).isEmpty();
    }

    private AssetPrice samplePrice(String symbol, String source, String quoteCurrency, Instant collectedAt) {
        return new AssetPrice(
                symbol,
                symbol,
                quoteCurrency,
                source,
                new BigDecimal("137500000"),
                new BigDecimal("0.018"),
                collectedAt
        );
    }
}
