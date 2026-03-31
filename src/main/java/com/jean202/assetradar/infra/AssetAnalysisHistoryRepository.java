package com.jean202.assetradar.infra;

import com.jean202.assetradar.analysis.AssetAnalysisSink;
import com.jean202.assetradar.domain.AssetAnalysis;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetAnalysisHistoryRepository implements AssetAnalysisSink {
    private final DatabaseClient databaseClient;

    public AssetAnalysisHistoryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> persist(AssetAnalysis analysis) {
        var spec = databaseClient.sql("""
                        INSERT INTO asset_analysis_history (
                            symbol,
                            quote_currency,
                            source,
                            current_price,
                            previous_price,
                            price_change,
                            change_rate,
                            movement,
                            analyzed_at
                        ) VALUES (
                            :symbol,
                            :quoteCurrency,
                            :source,
                            :currentPrice,
                            :previousPrice,
                            :priceChange,
                            :changeRate,
                            :movement,
                            :analyzedAt
                        )
                        """)
                .bind("symbol", analysis.symbol())
                .bind("quoteCurrency", analysis.quoteCurrency())
                .bind("source", analysis.source())
                .bind("currentPrice", analysis.currentPrice())
                .bind("priceChange", analysis.priceChange())
                .bind("changeRate", analysis.changeRate())
                .bind("movement", analysis.movement())
                .bind("analyzedAt", analysis.analyzedAt());

        if (analysis.previousPrice() == null) {
            spec = spec.bindNull("previousPrice", java.math.BigDecimal.class);
        } else {
            spec = spec.bind("previousPrice", analysis.previousPrice());
        }

        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public String sinkName() {
        return "analysis-postgres";
    }
}
