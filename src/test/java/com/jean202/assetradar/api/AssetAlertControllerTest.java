package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.query.AssetAlertHistoryQuery;
import com.jean202.assetradar.query.AssetAlertHistoryReader;
import com.jean202.assetradar.query.LatestAssetAlertQuery;
import com.jean202.assetradar.query.LatestAssetAlertReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AssetAlertControllerTest {
    @Test
    void latestReturnsLatestAlerts() {
        RecordingLatestReader latestReader = new RecordingLatestReader(List.of(
                alert("BTC", "CRITICAL", "PRICE_SURGE", "UP", "0.030", "0.020", "2026-03-31T00:00:00Z"),
                alert("ETH", "WARN", "PRICE_DROP", "DOWN", "-0.015", "0.010", "2026-03-31T00:00:01Z")
        ));
        AssetAlertController controller = new AssetAlertController(latestReader, query -> Flux.empty());

        AlertsResponse response = controller.latest("upbit", "krw", List.of("btc"), List.of("critical,warn")).block();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(2);
        assertThat(latestReader.lastQuery()).isEqualTo(
                new LatestAssetAlertQuery("UPBIT", "KRW", List.of("BTC"), List.of("CRITICAL", "WARN"))
        );
    }

    @Test
    void historyReturnsBoundedAlertHistory() {
        RecordingHistoryReader historyReader = new RecordingHistoryReader(List.of(
                alert("BTC", "WARN", "PRICE_SURGE", "UP", "0.015", "0.010", "2026-03-31T00:00:01Z")
        ));
        AssetAlertController controller = new AssetAlertController(query -> Flux.empty(), historyReader);

        AlertHistoryResponse response = controller.history(
                "btc",
                "upbit",
                "krw",
                List.of("warn,critical"),
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                5000
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(1);
        assertThat(historyReader.lastQuery()).isEqualTo(new AssetAlertHistoryQuery(
                "BTC",
                "UPBIT",
                "KRW",
                List.of("WARN", "CRITICAL"),
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                1000
        ));
    }

    private AssetAlert alert(
            String symbol,
            String severity,
            String alertType,
            String movement,
            String changeRate,
            String thresholdRate,
            String alertedAt
    ) {
        return new AssetAlert(
                symbol,
                "KRW",
                "UPBIT",
                alertType,
                severity,
                movement,
                new BigDecimal("100"),
                new BigDecimal("90"),
                new BigDecimal("10"),
                new BigDecimal(changeRate),
                new BigDecimal(thresholdRate),
                Instant.parse("2026-03-30T23:59:01Z"),
                60L,
                "message",
                Instant.parse(alertedAt)
        );
    }

    private static final class RecordingLatestReader implements LatestAssetAlertReader {
        private final List<AssetAlert> alerts;
        private LatestAssetAlertQuery lastQuery;

        private RecordingLatestReader(List<AssetAlert> alerts) {
            this.alerts = alerts;
        }

        @Override
        public Flux<AssetAlert> readLatest(LatestAssetAlertQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(alerts);
        }

        LatestAssetAlertQuery lastQuery() {
            return lastQuery;
        }
    }

    private static final class RecordingHistoryReader implements AssetAlertHistoryReader {
        private final List<AssetAlert> alerts;
        private AssetAlertHistoryQuery lastQuery;

        private RecordingHistoryReader(List<AssetAlert> alerts) {
            this.alerts = alerts;
        }

        @Override
        public Flux<AssetAlert> readHistory(AssetAlertHistoryQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(alerts);
        }

        AssetAlertHistoryQuery lastQuery() {
            return lastQuery;
        }
    }
}
