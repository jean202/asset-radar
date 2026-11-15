package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.StatisticsQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PostgresAssetStatisticsReader가 통계 계산용 가격 윈도우를 실제 SQL로 뽑아오는지 검증한다.
 * 통계 계산 자체는 StatisticsCalculator의 몫이고, 여기서는 윈도우 경계와 정렬이 관심사다.
 */
class PostgresAssetStatisticsReaderIntegrationTest extends AbstractPostgresIntegrationTest {

    private AssetPriceHistoryRepository repository;
    private PostgresAssetStatisticsReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetPriceHistoryRepository(databaseClient);
        reader = new PostgresAssetStatisticsReader(databaseClient);
    }

    @Test
    void readPriceWindowReturnsEmptyWhenNoRowInWindow() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "137500000", Instant.parse("2026-05-17T09:00:00Z")))
                .block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).isEmpty();
    }

    @Test
    void readPriceWindowReturnsSingleRow() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "137500000", Instant.parse("2026-05-17T10:30:00Z")))
                .block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).price()).isEqualByComparingTo("137500000");
        assertThat(found.get(0).name()).isEqualTo("BTC");
    }

    /** 이동평균/변동성 계산이 시간순 입력을 전제하므로 오름차순 정렬이 깨지면 안 된다. */
    @Test
    void readPriceWindowOrdersByCollectedAtAscending() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "3", Instant.parse("2026-05-17T10:30:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "1", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "2", Instant.parse("2026-05-17T10:20:00Z"))).block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).extracting(AssetPrice::price)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
    }

    /** 윈도우 경계는 >= / <= 이므로 양 끝값은 포함되고 1초 바깥은 빠져야 한다. */
    @Test
    void readPriceWindowBoundsAreInclusiveAndExcludeOutsideRows() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "1", Instant.parse("2026-05-17T09:59:59Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "2", Instant.parse("2026-05-17T10:00:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "3", Instant.parse("2026-05-17T11:00:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "4", Instant.parse("2026-05-17T11:00:01Z"))).block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        "UPBIT",
                        "KRW",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).extracting(AssetPrice::collectedAt).containsExactly(
                Instant.parse("2026-05-17T10:00:00Z"),
                Instant.parse("2026-05-17T11:00:00Z"));
    }

    @Test
    void readPriceWindowFiltersBySourceAndQuoteCurrency() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "1", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", "2", Instant.parse("2026-05-17T10:20:00Z"))).block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        "BINANCE",
                        "USDT",
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).source()).isEqualTo("BINANCE");
        assertThat(found.get(0).quoteCurrency()).isEqualTo("USDT");
    }

    /** source/quoteCurrency가 없으면 조건절이 아예 붙지 않아 다른 소스도 함께 잡힌다. */
    @Test
    void readPriceWindowWithoutSourceReturnsEverySource() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "1", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", "2", Instant.parse("2026-05-17T10:20:00Z"))).block();

        List<AssetPrice> found = reader
                .readPriceWindow(new StatisticsQuery(
                        "BTC",
                        null,
                        null,
                        Instant.parse("2026-05-17T10:00:00Z"),
                        Instant.parse("2026-05-17T11:00:00Z")))
                .collectList()
                .block();

        assertThat(found).extracting(AssetPrice::source).containsExactly("UPBIT", "BINANCE");
    }

    private AssetPrice samplePrice(
            String symbol,
            String source,
            String quoteCurrency,
            String price,
            Instant collectedAt
    ) {
        return new AssetPrice(
                symbol,
                symbol,
                quoteCurrency,
                source,
                new BigDecimal(price),
                new BigDecimal("0.018"),
                collectedAt
        );
    }
}
