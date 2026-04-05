package com.jean202.assetradar.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatisticsQueryTest {
    private static final Instant NOW = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2025-01-02T00:00:00Z");

    @Test
    void validQuery() {
        StatisticsQuery q = new StatisticsQuery("btc", "UPBIT", "krw", NOW, LATER);
        assertThat(q.symbol()).isEqualTo("BTC");
        assertThat(q.source()).isEqualTo("UPBIT");
        assertThat(q.quoteCurrency()).isEqualTo("KRW");
    }

    @Test
    void symbolRequired() {
        assertThatThrownBy(() -> new StatisticsQuery(null, null, null, NOW, LATER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("symbol");
    }

    @Test
    void fromRequired() {
        assertThatThrownBy(() -> new StatisticsQuery("BTC", null, null, null, LATER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    void fromMustBeBeforeTo() {
        assertThatThrownBy(() -> new StatisticsQuery("BTC", null, null, LATER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    void optionalFieldsCanBeNull() {
        StatisticsQuery q = new StatisticsQuery("BTC", null, null, NOW, LATER);
        assertThat(q.source()).isNull();
        assertThat(q.quoteCurrency()).isNull();
    }
}
