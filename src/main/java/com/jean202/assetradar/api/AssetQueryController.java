package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetHistoryQuery;
import com.jean202.assetradar.query.AssetPriceHistoryReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@Tag(name = "Assets", description = "최신 가격과 가격 이력 조회")
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
    @Operation(summary = "최신 가격 조회", description = "Redis 최신 가격 캐시에서 source, quoteCurrency, symbols 조건에 맞는 자산 가격을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "최신 자산 가격 목록",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LatestAssetsResponse.class),
                    examples = @ExampleObject(name = "latest-upbit-assets", value = OpenApiExamples.LATEST_ASSETS)
            )
    )
    public Mono<LatestAssetsResponse> latest(
            @Parameter(description = "데이터 소스. 예: UPBIT, KIS, GOLDAPI", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화. 예: KRW, USD", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회할 심볼 목록. 콤마 구분 또는 반복 query param 사용", example = "BTC,ETH")
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
    @Operation(summary = "가격 이력 조회", description = "PostgreSQL 가격 이력 테이블에서 특정 자산의 가격 시계열을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "가격 이력",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssetHistoryResponse.class),
                    examples = @ExampleObject(name = "btc-history", value = OpenApiExamples.ASSET_HISTORY)
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
    public Mono<AssetHistoryResponse> history(
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
