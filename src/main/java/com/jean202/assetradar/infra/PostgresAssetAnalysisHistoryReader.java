package com.jean202.assetradar.infra;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.AssetAnalysisHistoryQuery;
import com.jean202.assetradar.query.AssetAnalysisHistoryReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class PostgresAssetAnalysisHistoryReader implements AssetAnalysisHistoryReader {
    private final DatabaseClient databaseClient;

    public PostgresAssetAnalysisHistoryReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<AssetAnalysis> readHistory(AssetAnalysisHistoryQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    symbol,
                    quote_currency,
                    source,
                    current_price,
                    previous_price,
                    price_change,
                    change_rate,
                    movement,
                    analyzed_at
                FROM asset_analysis_history
                WHERE symbol = :symbol
                """);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("symbol", query.symbol());

        if (query.source() != null) {
            sql.append(" AND source = :source");
            bindings.put("source", query.source());
        }

        if (query.quoteCurrency() != null) {
            sql.append(" AND quote_currency = :quoteCurrency");
            bindings.put("quoteCurrency", query.quoteCurrency());
        }

        if (query.from() != null) {
            sql.append(" AND analyzed_at >= :from");
            bindings.put("from", query.from());
        }

        if (query.to() != null) {
            sql.append(" AND analyzed_at <= :to");
            bindings.put("to", query.to());
        }

        sql.append(" ORDER BY analyzed_at DESC LIMIT :limit");
        bindings.put("limit", query.limit());

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, metadata) -> new AssetAnalysis(
                row.get("symbol", String.class),
                row.get("quote_currency", String.class),
                row.get("source", String.class),
                row.get("current_price", BigDecimal.class),
                row.get("previous_price", BigDecimal.class),
                row.get("price_change", BigDecimal.class),
                row.get("change_rate", BigDecimal.class),
                row.get("movement", String.class),
                toInstant(row.get("analyzed_at", OffsetDateTime.class))
        )).all();
    }

    private Instant toInstant(OffsetDateTime analyzedAt) {
        return analyzedAt == null ? null : analyzedAt.toInstant();
    }
}
