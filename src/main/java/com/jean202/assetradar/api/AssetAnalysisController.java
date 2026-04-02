package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetAnalysisHistoryQuery;
import com.jean202.assetradar.query.AssetAnalysisHistoryReader;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analysis")
public class AssetAnalysisController {
    private final LatestAssetAnalysisReader latestAssetAnalysisReader;
    private final AssetAnalysisHistoryReader assetAnalysisHistoryReader;

    public AssetAnalysisController(
            LatestAssetAnalysisReader latestAssetAnalysisReader,
            AssetAnalysisHistoryReader assetAnalysisHistoryReader
    ) {
        this.latestAssetAnalysisReader = latestAssetAnalysisReader;
        this.assetAnalysisHistoryReader = assetAnalysisHistoryReader;
    }

    @GetMapping
    public Mono<AnalysisResponse> latest(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) List<String> symbols
    ) {
        LatestAssetAnalysisQuery query = new LatestAssetAnalysisQuery(source, quoteCurrency, symbols);

        return latestAssetAnalysisReader.readLatest(query)
                .collectList()
                .map(analyses -> new AnalysisResponse(
                        query.source(),
                        query.quoteCurrency(),
                        analyses.size(),
                        analyses
                ));
    }

    @GetMapping("/history")
    public Mono<AnalysisHistoryResponse> history(
            @RequestParam String symbol,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit
    ) {
        AssetAnalysisHistoryQuery query = new AssetAnalysisHistoryQuery(
                symbol,
                source,
                quoteCurrency,
                from,
                to,
                sanitizeLimit(limit)
        );

        return assetAnalysisHistoryReader.readHistory(query)
                .collectList()
                .map(analyses -> new AnalysisHistoryResponse(
                        query.symbol(),
                        query.source(),
                        query.quoteCurrency(),
                        query.from(),
                        query.to(),
                        analyses.size(),
                        analyses
                ));
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }

        return Math.min(Math.max(limit, 1), 1000);
    }
}
