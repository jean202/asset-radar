package com.jean202.assetradar.infra;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetStatisticsReader;
import com.jean202.assetradar.query.StatisticsQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class PostgresAssetStatisticsReader implements AssetStatisticsReader {
    private final DatabaseClient databaseClient;

    public PostgresAssetStatisticsReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<AssetPrice> readPriceWindow(StatisticsQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT symbol, quote_currency, source, price, signed_change_rate, collected_at
                FROM asset_price_history
                WHERE symbol = :symbol
                  AND collected_at >= :from
                  AND collected_at <= :to
                """);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("symbol", query.symbol());
        bindings.put("from", query.from());
        bindings.put("to", query.to());

        if (query.source() != null) {
            sql.append(" AND source = :source");
            bindings.put("source", query.source());
        }
        if (query.quoteCurrency() != null) {
            sql.append(" AND quote_currency = :quoteCurrency");
            bindings.put("quoteCurrency", query.quoteCurrency());
        }

        sql.append(" ORDER BY collected_at ASC LIMIT 100000");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, metadata) -> new AssetPrice(
                row.get("symbol", String.class),
                row.get("quote_currency", String.class),
                row.get("source", String.class),
                row.get("price", BigDecimal.class),
                row.get("signed_change_rate", BigDecimal.class),
                toInstant(row.get("collected_at", OffsetDateTime.class))
        )).all();
    }

    private Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
