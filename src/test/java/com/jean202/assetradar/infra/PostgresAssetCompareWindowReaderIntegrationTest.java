package com.jean202.assetradar.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindow;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PostgresAssetCompareWindowReader는 시작가/종료가/표본 수를 각각 다른 SQL로 뽑아 zip한다.
 * 세 쿼리가 같은 필터를 공유하는지, 경계 조건에서 서로 어긋나지 않는지가 관심사다.
 */
class PostgresAssetCompareWindowReaderIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-05-17T10:00:00Z");
    private static final Instant TO = Instant.parse("2026-05-17T11:00:00Z");

    private AssetPriceHistoryRepository repository;
    private PostgresAssetCompareWindowReader reader;

    @BeforeEach
    void setUpAdapters() {
        repository = new AssetPriceHistoryRepository(databaseClient);
        reader = new PostgresAssetCompareWindowReader(databaseClient);
    }

    @Test
    void readWindowReturnsFirstAndLastPriceWithSampleCount() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "100", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "120", Instant.parse("2026-05-17T10:30:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "150", Instant.parse("2026-05-17T10:50:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.startPrice().price()).isEqualByComparingTo("100");
        assertThat(window.startPrice().collectedAt()).isEqualTo(Instant.parse("2026-05-17T10:10:00Z"));
        assertThat(window.endPrice().price()).isEqualByComparingTo("150");
        assertThat(window.endPrice().collectedAt()).isEqualTo(Instant.parse("2026-05-17T10:50:00Z"));
        assertThat(window.sampleCount()).isEqualTo(3L);
        assertThat(window.asset().symbol()).isEqualTo("BTC");
    }

    /** 표본이 하나면 시작가와 종료가가 같은 행을 가리켜야 한다. */
    @Test
    void readWindowWithSingleRowUsesSameRowForStartAndEnd() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "137500000", Instant.parse("2026-05-17T10:30:00Z")))
                .block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(1L);
        assertThat(window.startPrice().collectedAt()).isEqualTo(window.endPrice().collectedAt());
        assertThat(window.startPrice().price()).isEqualByComparingTo("137500000");
    }

    /** 경계 SQL이 empty Mono를 내면 zip이 통째로 비므로 readWindow 자체가 빈 Mono가 된다. */
    @Test
    void readWindowIsEmptyWhenNoRowInWindow() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "100", Instant.parse("2026-05-17T09:00:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNull();
    }

    @Test
    void readWindowIsEmptyWhenTableIsEmpty() {
        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNull();
    }

    /** 윈도우 바깥 데이터가 시작가/종료가/표본 수 어디에도 섞이면 안 된다. */
    @Test
    void readWindowExcludesRowsOutsideWindowBounds() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "1", Instant.parse("2026-05-17T09:59:59Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "2", FROM)).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "3", TO)).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "4", Instant.parse("2026-05-17T11:00:01Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(2L);
        assertThat(window.startPrice().price()).isEqualByComparingTo("2");
        assertThat(window.endPrice().price()).isEqualByComparingTo("3");
    }

    /** 다른 소스의 같은 심볼이 경계값과 표본 수를 오염시키면 안 된다. */
    @Test
    void readWindowFiltersBySourceAndQuoteCurrency() {
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", "1", Instant.parse("2026-05-17T10:05:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "2", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "3", Instant.parse("2026-05-17T10:50:00Z"))).block();
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", "4", Instant.parse("2026-05-17T10:55:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(2L);
        assertThat(window.startPrice().price()).isEqualByComparingTo("2");
        assertThat(window.endPrice().price()).isEqualByComparingTo("3");
    }

    /** source/quoteCurrency가 없으면 조건절이 빠져 모든 소스가 한 윈도우로 합쳐진다. */
    @Test
    void readWindowWithoutSourceAggregatesEverySource() {
        repository.persist(samplePrice("BTC", "BINANCE", "USDT", "1", Instant.parse("2026-05-17T10:05:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "2", Instant.parse("2026-05-17T10:10:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "3", Instant.parse("2026-05-17T10:50:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", null, null), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(3L);
        assertThat(window.startPrice().source()).isEqualTo("BINANCE");
        assertThat(window.endPrice().source()).isEqualTo("UPBIT");
    }

    /** 같은 시각 행이 둘이면 LIMIT 1이 어느 쪽을 집든 표본 수는 정확해야 한다. */
    @Test
    void readWindowCountsRowsSharingTheSameTimestamp() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "10", Instant.parse("2026-05-17T10:30:00Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "20", Instant.parse("2026-05-17T10:30:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"), FROM, TO)
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(2L);
        assertThat(window.startPrice().collectedAt()).isEqualTo(Instant.parse("2026-05-17T10:30:00Z"));
        assertThat(window.endPrice().collectedAt()).isEqualTo(Instant.parse("2026-05-17T10:30:00Z"));
    }

    /** from == to 윈도우는 그 시각의 행만 잡아야 한다. */
    @Test
    void readWindowWithZeroLengthWindowMatchesExactTimestamp() {
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "10", Instant.parse("2026-05-17T10:29:59Z"))).block();
        repository.persist(samplePrice("BTC", "UPBIT", "KRW", "20", Instant.parse("2026-05-17T10:30:00Z"))).block();

        AssetCompareWindow window = reader
                .readWindow(
                        new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                        Instant.parse("2026-05-17T10:30:00Z"),
                        Instant.parse("2026-05-17T10:30:00Z"))
                .block();

        assertThat(window).isNotNull();
        assertThat(window.sampleCount()).isEqualTo(1L);
        assertThat(window.startPrice().price()).isEqualByComparingTo("20");
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
