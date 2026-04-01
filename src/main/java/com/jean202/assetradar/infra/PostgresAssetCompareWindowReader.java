package com.jean202.assetradar.infra;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindow;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PostgresAssetCompareWindowReader implements AssetCompareWindowReader {
    private final DatabaseClient databaseClient;

    public PostgresAssetCompareWindowReader(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<AssetCompareWindow> readWindow(AssetCompareQuery.AssetSpec asset, Instant from, Instant to) {
        Mono<AssetPrice> startPrice = fetchBoundary(asset, from, to, "ASC");
        Mono<AssetPrice> endPrice = fetchBoundary(asset, from, to, "DESC");
        Mono<Long> sampleCount = countSamples(asset, from, to);

        return Mono.zip(startPrice, endPrice, sampleCount)
                .map(tuple -> new AssetCompareWindow(
                        asset,
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3()
                ));
    }

    private Mono<AssetPrice> fetchBoundary(
            AssetCompareQuery.AssetSpec asset,
            Instant from,
            Instant to,
            String direction
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    symbol,
                    quote_currency,
                    source,
                    price,
                    signed_change_rate,
                    collected_at
                FROM asset_price_history
                WHERE symbol = :symbol
                  AND collected_at >= :from
                  AND collected_at <= :to
                """);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("symbol", asset.symbol());
        bindings.put("from", from);
        bindings.put("to", to);

        appendOptionalFilters(asset, sql, bindings);
        sql.append(" ORDER BY collected_at ").append(direction).append(" LIMIT 1");

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
        )).one();
    }

    private Mono<Long> countSamples(AssetCompareQuery.AssetSpec asset, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS sample_count
                FROM asset_price_history
                WHERE symbol = :symbol
                  AND collected_at >= :from
                  AND collected_at <= :to
                """);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("symbol", asset.symbol());
        bindings.put("from", from);
        bindings.put("to", to);

        appendOptionalFilters(asset, sql, bindings);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, metadata) -> row.get("sample_count", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private void appendOptionalFilters(
            AssetCompareQuery.AssetSpec asset,
            StringBuilder sql,
            Map<String, Object> bindings
    ) {
        if (asset.source() != null) {
            sql.append(" AND source = :source");
            bindings.put("source", asset.source());
        }

        if (asset.quoteCurrency() != null) {
            sql.append(" AND quote_currency = :quoteCurrency");
            bindings.put("quoteCurrency", asset.quoteCurrency());
        }
    }

    private Instant toInstant(OffsetDateTime collectedAt) {
        return collectedAt == null ? null : collectedAt.toInstant();
    }
}
