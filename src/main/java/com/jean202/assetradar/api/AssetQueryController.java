package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetHistoryQuery;
import com.jean202.assetradar.query.AssetPriceHistoryReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AssetQueryController {
    private final LatestAssetPriceReader latestAssetPriceReader;
    private final AssetPriceHistoryReader assetPriceHistoryReader;

    public AssetQueryController(
            LatestAssetPriceReader latestAssetPriceReader,
            AssetPriceHistoryReader assetPriceHistoryReader
    ) {
        this.latestAssetPriceReader = latestAssetPriceReader;
        this.assetPriceHistoryReader = assetPriceHistoryReader;
    }

    @GetMapping("/latest")
    public Mono<LatestAssetsResponse> latest(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) List<String> symbols
    ) {
        LatestAssetQuery query = new LatestAssetQuery(source, quoteCurrency, symbols);

        return latestAssetPriceReader.readLatest(query)
                .collectList()
                .map(prices -> new LatestAssetsResponse(
                        query.source(),
                        query.quoteCurrency(),
                        prices.size(),
                        prices
                ));
    }

    @GetMapping("/history")
    public Mono<AssetHistoryResponse> history(
            @RequestParam String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit
    ) {
        AssetHistoryQuery query = new AssetHistoryQuery(
                symbol,
                source,
                quoteCurrency,
                from,
                to,
                sanitizeLimit(limit)
        );

        return assetPriceHistoryReader.readHistory(query)
                .collectList()
                .map(prices -> new AssetHistoryResponse(
                        query.symbol(),
                        query.source(),
                        query.quoteCurrency(),
                        query.from(),
                        query.to(),
                        prices.size(),
                        prices
                ));
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }

        return Math.min(Math.max(limit, 1), 1000);
    }
}
