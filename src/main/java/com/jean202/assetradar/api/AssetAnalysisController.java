package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetAnalysisHistoryQuery;
import com.jean202.assetradar.query.AssetAnalysisHistoryReader;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Analysis", description = "최신 분석 결과와 분석 이력 조회")
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
    @Operation(summary = "최신 분석 조회", description = "최신 가격 변화 분석 결과를 Redis 캐시에서 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "최신 분석 결과",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AnalysisResponse.class),
                    examples = @ExampleObject(name = "latest-analysis", value = OpenApiExamples.ANALYSIS)
            )
    )
    public Mono<AnalysisResponse> latest(
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회할 심볼 목록", example = "BTC,ETH")
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
    @Operation(summary = "분석 이력 조회", description = "PostgreSQL 분석 이력 테이블에서 특정 자산의 분석 결과를 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "분석 이력",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AnalysisHistoryResponse.class),
                    examples = @ExampleObject(name = "analysis-history", value = OpenApiExamples.ANALYSIS_HISTORY)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "필수 query param 누락 또는 잘못된 날짜 형식",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-request", value = OpenApiExamples.ERROR_INVALID_REQUEST)
            )
    )
    public Mono<AnalysisHistoryResponse> history(
            @Parameter(description = "자산 심볼", example = "BTC", required = true)
            @RequestParam String symbol,
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회 시작 시각, ISO-8601", example = "2026-05-17T09:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "조회 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "조회 개수. 1~1000으로 보정됨", example = "100")
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
