package com.jean202.assetradar.infra;

import com.jean202.assetradar.domain.AssetAlert;
import com.jean202.assetradar.query.AssetAlertHistoryQuery;
import com.jean202.assetradar.query.AssetAlertHistoryReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class PostgresAssetAlertHistoryReader implements AssetAlertHistoryReader {
    private final DatabaseClient databaseClient;

    public PostgresAssetAlertHistoryReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<AssetAlert> readHistory(AssetAlertHistoryQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    symbol,
                    quote_currency,
                    source,
                    alert_type,
                    severity,
                    movement,
                    current_price,
                    previous_price,
                    price_change,
                    change_rate,
                    threshold_rate,
                    baseline_at,
                    window_seconds,
                    message,
                    alerted_at
                FROM asset_alert_history
                WHERE 1 = 1
                """);
        Map<String, Object> bindings = new LinkedHashMap<>();

        if (query.symbol() != null) {
            sql.append(" AND symbol = :symbol");
            bindings.put("symbol", query.symbol());
        }

        if (query.source() != null) {
            sql.append(" AND source = :source");
            bindings.put("source", query.source());
        }

        if (query.quoteCurrency() != null) {
            sql.append(" AND quote_currency = :quoteCurrency");
            bindings.put("quoteCurrency", query.quoteCurrency());
        }

        if (!query.severities().isEmpty()) {
            sql.append(" AND severity IN (");
            int index = 0;
            for (String severity : query.severities()) {
                if (index > 0) {
                    sql.append(", ");
                }
                String bindingName = "severity" + index;
                sql.append(':').append(bindingName);
                bindings.put(bindingName, severity);
                index++;
            }
            sql.append(')');
        }

        if (query.from() != null) {
            sql.append(" AND alerted_at >= :from");
            bindings.put("from", query.from());
        }

        if (query.to() != null) {
            sql.append(" AND alerted_at <= :to");
            bindings.put("to", query.to());
        }

        sql.append(" ORDER BY alerted_at DESC LIMIT :limit");
        bindings.put("limit", query.limit());

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, metadata) -> new AssetAlert(
                row.get("symbol", String.class),
                row.get("quote_currency", String.class),
                row.get("source", String.class),
                row.get("alert_type", String.class),
                row.get("severity", String.class),
                row.get("movement", String.class),
                row.get("current_price", BigDecimal.class),
                row.get("previous_price", BigDecimal.class),
                row.get("price_change", BigDecimal.class),
                row.get("change_rate", BigDecimal.class),
                row.get("threshold_rate", BigDecimal.class),
                toInstant(row.get("baseline_at", OffsetDateTime.class)),
                row.get("window_seconds", Long.class),
                row.get("message", String.class),
                toInstant(row.get("alerted_at", OffsetDateTime.class))
        )).all();
    }

    private Instant toInstant(OffsetDateTime alertedAt) {
        return alertedAt == null ? null : alertedAt.toInstant();
    }
}
