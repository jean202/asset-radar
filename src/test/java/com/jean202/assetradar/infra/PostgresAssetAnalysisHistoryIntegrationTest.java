package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.AssetAnalysisHistoryQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AssetAnalysisHistoryRepository(쓰기)와 PostgresAssetAnalysisHistoryReader(읽기)를
 * 실제 스키마가 올라간 Postgres에 붙여 SQL 문법과 컬럼 매핑을 검증한다.
 */
class PostgresAssetAnalysisHistoryIntegrationTest extends AbstractPostgresIntegrationTest {

    private AssetAnalysisHistoryRepository repository;
    private PostgresAssetAnalysisHistoryReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetAnalysisHistoryRepository(databaseClient);
        reader = new PostgresAssetAnalysisHistoryReader(databaseClient);
    }

    @Test
    void persistedAnalysisIsReadBackWithAllColumnsMapped() {
        AssetAnalysis analysis = new AssetAnalysis(
                "BTC",
                "KRW",
                "UPBIT",
                new BigDecimal("137500000.00000000"),
                new BigDecimal("136900000.00000000"),
                new BigDecimal("600000.00000000"),
                new BigDecimal("0.00438276"),
                "UP",
                Instant.parse("2026-05-17T10:15:03Z")
        );

        repository.persist(analysis).block();

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery("BTC", "UPBIT", "KRW", null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        AssetAnalysis row = found.get(0);
        assertThat(row.symbol()).isEqualTo("BTC");
        assertThat(row.quoteCurrency()).isEqualTo("KRW");
        assertThat(row.source()).isEqualTo("UPBIT");
        assertThat(row.currentPrice()).isEqualByComparingTo("137500000");
        assertThat(row.previousPrice()).isEqualByComparingTo("136900000");
        assertThat(row.priceChange()).isEqualByComparingTo("600000");
        assertThat(row.changeRate()).isEqualByComparingTo("0.00438276");
        assertThat(row.movement()).isEqualTo("UP");
        assertThat(row.analyzedAt()).isEqualTo(Instant.parse("2026-05-17T10:15:03Z"));
    }

    @Test
    void readHistoryReturnsEmptyWhenNoRowMatches() {
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:03Z"))).block();

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery("ETH", null, null, null, null, 10))
                .collectList()
                .block();

        assertThat(found).isEmpty();
    }

    @Test
    void readHistoryFiltersBySourceAndQuoteCurrency() {
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(sampleAnalysis("BTC", "BINANCE", "USDT", Instant.parse("2026-05-17T10:15:04Z"))).block();

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery("BTC", "BINANCE", "USDT", null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).source()).isEqualTo("BINANCE");
    }

    @Test
    void readHistoryOrdersByAnalyzedAtDescendingAndAppliesLimit() {
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:01Z"))).block();
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:03Z"))).block();
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery("BTC", "UPBIT", "KRW", null, null, 2))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAnalysis::analyzedAt).containsExactly(
                Instant.parse("2026-05-17T10:15:03Z"),
                Instant.parse("2026-05-17T10:15:02Z"));
    }

    /** from/to는 SQL에서 >= 와 <= 라 경계값이 포함되어야 한다. */
    @Test
    void readHistoryWindowBoundsAreInclusive() {
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T09:59:59Z"))).block();
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:00:00Z"))).block();
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T11:00:00Z"))).block();
        repository.persist(sampleAnalysis("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T11:00:01Z"))).block();

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z"),
                        10))
                .collectList()
                .block();

        assertThat(found).extracting(AssetAnalysis::analyzedAt).containsExactly(
                Instant.parse("2026-05-17T11:00:00Z"),
                Instant.parse("2026-05-17T10:00:00Z"));
    }

    /** previous_price는 NULL 허용 컬럼이라 첫 분석 결과가 그대로 왕복되어야 한다. */
    @Test
    void persistAndReadBackNullPreviousPrice() {
        repository.persist(new AssetAnalysis(
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

        List<AssetAnalysis> found = reader
                .readHistory(new AssetAnalysisHistoryQuery("BTC", null, null, null, null, 10))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).previousPrice()).isNull();
        assertThat(found.get(0).movement()).isEqualTo("FLAT");
    }

    private AssetAnalysis sampleAnalysis(String symbol, String source, String quoteCurrency, Instant analyzedAt) {
        return new AssetAnalysis(
                symbol,
                quoteCurrency,
                source,
                new BigDecimal("137500000"),
                new BigDecimal("136900000"),
                new BigDecimal("600000"),
                new BigDecimal("0.00438276"),
                "UP",
                analyzedAt
        );
    }
}
