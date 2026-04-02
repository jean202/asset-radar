package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetAlertHistoryQuery;
import com.jean202.assetradar.query.AssetAlertHistoryReader;
import com.jean202.assetradar.query.LatestAssetAlertQuery;
import com.jean202.assetradar.query.LatestAssetAlertReader;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/alerts")
public class AssetAlertController {
    private final LatestAssetAlertReader latestAssetAlertReader;
    private final AssetAlertHistoryReader assetAlertHistoryReader;

    public AssetAlertController(
            LatestAssetAlertReader latestAssetAlertReader,
            AssetAlertHistoryReader assetAlertHistoryReader
    ) {
        this.latestAssetAlertReader = latestAssetAlertReader;
        this.assetAlertHistoryReader = assetAlertHistoryReader;
    }

    @GetMapping
    public Mono<AlertsResponse> latest(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) List<String> symbols,
            @RequestParam(required = false) List<String> severities
    ) {
        LatestAssetAlertQuery query = new LatestAssetAlertQuery(source, quoteCurrency, symbols, severities);

        return latestAssetAlertReader.readLatest(query)
                .collectList()
                .map(alerts -> new AlertsResponse(
                        query.source(),
                        query.quoteCurrency(),
                        alerts.size(),
                        alerts
                ));
    }

    @GetMapping("/history")
    public Mono<AlertHistoryResponse> history(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) List<String> severities,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit
    ) {
        AssetAlertHistoryQuery query = new AssetAlertHistoryQuery(
                symbol,
                source,
                quoteCurrency,
                severities,
                from,
                to,
                sanitizeLimit(limit)
        );

        return assetAlertHistoryReader.readHistory(query)
                .collectList()
                .map(alerts -> new AlertHistoryResponse(
                        query.symbol(),
                        query.source(),
                        query.quoteCurrency(),
                        query.from(),
                        query.to(),
                        alerts.size(),
                        alerts
                ));
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }

        return Math.min(Math.max(limit, 1), 1000);
    }
}
