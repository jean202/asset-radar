package com.jean202.assetradar.infra;

import com.jean202.assetradar.alert.AssetAlertSink;
import com.jean202.assetradar.domain.AssetAlert;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AssetAlertHistoryRepository implements AssetAlertSink {
    private final DatabaseClient databaseClient;

    public AssetAlertHistoryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> persist(AssetAlert alert) {
        var spec = databaseClient.sql("""
                        INSERT INTO asset_alert_history (
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
                        ) VALUES (
                            :symbol,
                            :quoteCurrency,
                            :source,
                            :alertType,
                            :severity,
                            :movement,
                            :currentPrice,
                            :previousPrice,
                            :priceChange,
                            :changeRate,
                            :thresholdRate,
                            :baselineAt,
                            :windowSeconds,
                            :message,
                            :alertedAt
                        )
                        """)
                .bind("symbol", alert.symbol())
                .bind("quoteCurrency", alert.quoteCurrency())
                .bind("source", alert.source())
                .bind("alertType", alert.alertType())
                .bind("severity", alert.severity())
                .bind("movement", alert.movement())
                .bind("currentPrice", alert.currentPrice())
                .bind("priceChange", alert.priceChange())
                .bind("changeRate", alert.changeRate())
                .bind("thresholdRate", alert.thresholdRate())
                .bind("baselineAt", alert.baselineAt())
                .bind("windowSeconds", alert.windowSeconds())
                .bind("message", alert.message())
                .bind("alertedAt", alert.alertedAt());

        if (alert.previousPrice() == null) {
            spec = spec.bindNull("previousPrice", java.math.BigDecimal.class);
        } else {
            spec = spec.bind("previousPrice", alert.previousPrice());
        }

        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public String sinkName() {
        return "alert-postgres";
    }
}
