package com.jean202.assetradar.infra;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.pipeline.AssetPriceSink;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetPriceHistoryRepository implements AssetPriceSink {
    private final DatabaseClient databaseClient;

    public AssetPriceHistoryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> persist(AssetPrice price) {
        return databaseClient.sql("""
                        INSERT INTO asset_price_history (
                            symbol,
                            quote_currency,
                            source,
                            price,
                            signed_change_rate,
                            collected_at
                        ) VALUES (
                            :symbol,
                            :quoteCurrency,
                            :source,
                            :price,
                            :signedChangeRate,
                            :collectedAt
                        )
                        """)
                .bind("symbol", price.symbol())
                .bind("quoteCurrency", price.quoteCurrency())
                .bind("source", price.source())
                .bind("price", price.price())
                .bind("signedChangeRate", price.signedChangeRate())
                .bind("collectedAt", price.collectedAt())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public String sinkName() {
        return "postgres";
    }
}
