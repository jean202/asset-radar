package com.jean202.assetradar.infra;

import com.jean202.assetradar.query.DashboardHistoryMetrics;
import com.jean202.assetradar.query.DashboardHistoryMetricsReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostgresDashboardHistoryMetricsReader implements DashboardHistoryMetricsReader {
    private final DatabaseClient databaseClient;

    public PostgresDashboardHistoryMetricsReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<DashboardHistoryMetrics> readMetrics() {
        return databaseClient.sql("""
                        SELECT
                            COUNT(*) AS history_row_count,
                            MAX(collected_at) AS latest_collected_at
                        FROM asset_price_history
                        """)
                .map((row, metadata) -> new DashboardHistoryMetrics(
                        row.get("history_row_count", Long.class),
                        toInstant(row.get("latest_collected_at", OffsetDateTime.class))
                ))
                .one()
                .defaultIfEmpty(new DashboardHistoryMetrics(0, null));
    }

    private Instant toInstant(OffsetDateTime collectedAt) {
        return collectedAt == null ? null : collectedAt.toInstant();
    }
}
