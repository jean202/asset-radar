package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.DashboardHistoryMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PostgresDashboardHistoryMetricsReader의 집계 SQL을 실제 테이블에 붙여 검증한다.
 * 빈 테이블에서 COUNT는 0, MAX는 NULL로 나오는 조합이 핵심 경계다.
 */
class PostgresDashboardHistoryMetricsReaderIntegrationTest extends AbstractPostgresIntegrationTest {

    private AssetPriceHistoryRepository repository;
    private PostgresDashboardHistoryMetricsReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetPriceHistoryRepository(databaseClient);
        reader = new PostgresDashboardHistoryMetricsReader(databaseClient);
    }

    /** 집계 쿼리는 빈 테이블에서도 행 하나를 내므로 count=0, MAX=NULL이 그대로 매핑되어야 한다. */
    @Test
    void readMetricsOnEmptyTableReturnsZeroCountAndNullTimestamp() {
        DashboardHistoryMetrics metrics = reader.readMetrics().block();

        assertThat(metrics).isNotNull();
        assertThat(metrics.historyRowCount()).isZero();
        assertThat(metrics.latestCollectedAt()).isNull();
    }

    @Test
    void readMetricsWithSingleRowReturnsThatRowTimestamp() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();

        DashboardHistoryMetrics metrics = reader.readMetrics().block();

        assertThat(metrics).isNotNull();
        assertThat(metrics.historyRowCount()).isEqualTo(1L);
        assertThat(metrics.latestCollectedAt()).isEqualTo(Instant.parse("2026-05-17T10:15:02Z"));
    }

    /** MAX는 전체 소스를 가로질러 가장 최근 수집 시각을 잡아야 한다. */
    @Test
    void readMetricsCountsEverySourceAndPicksLatestCollectedAt() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:02Z"))).block();
        repository.persist(samplePrice("ETH", "UPBIT", "KRW", Instant.parse("2026-05-17T10:15:07Z"))).block();
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", Instant.parse("2026-05-17T10:15:05Z"))).block();

        DashboardHistoryMetrics metrics = reader.readMetrics().block();

        assertThat(metrics).isNotNull();
        assertThat(metrics.historyRowCount()).isEqualTo(3L);
        assertThat(metrics.latestCollectedAt()).isEqualTo(Instant.parse("2026-05-17T10:15:07Z"));
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
