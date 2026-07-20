package com.jean202.assetradar.api;

import com.jean202.assetradar.query.AssetAlertHistoryQuery;
import com.jean202.assetradar.query.AssetAlertHistoryReader;
import com.jean202.assetradar.query.LatestAssetAlertQuery;
import com.jean202.assetradar.query.LatestAssetAlertReader;
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
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "최신 알림과 알림 이력 조회")
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
    @Operation(summary = "최신 알림 조회", description = "최신 알림 캐시에서 source, quoteCurrency, symbols, severities 조건에 맞는 알림을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "최신 알림 목록",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AlertsResponse.class),
                    examples = @ExampleObject(name = "latest-alerts", value = OpenApiExamples.ALERTS)
            )
    )
    public Mono<AlertsResponse> latest(
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "조회할 심볼 목록", example = "BTC,ETH")
            @RequestParam(required = false) List<String> symbols,
            @Parameter(description = "알림 심각도 목록. 예: INFO, WARN, CRITICAL", example = "WARN,CRITICAL")
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
    @Operation(summary = "알림 이력 조회", description = "PostgreSQL 알림 이력 테이블에서 조건에 맞는 알림 이벤트를 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "알림 이력",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AlertHistoryResponse.class),
                    examples = @ExampleObject(name = "alert-history", value = OpenApiExamples.ALERT_HISTORY)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 날짜 형식 또는 query param",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(name = "invalid-request", value = OpenApiExamples.ERROR_INVALID_REQUEST)
            )
    )
    public Mono<AlertHistoryResponse> history(
            @Parameter(description = "자산 심볼", example = "BTC")
            @RequestParam(required = false) String symbol,
            @Parameter(description = "데이터 소스", example = "UPBIT")
            @RequestParam(required = false) String source,
            @Parameter(description = "기준 통화", example = "KRW")
            @RequestParam(required = false) String quoteCurrency,
            @Parameter(description = "알림 심각도 목록", example = "WARN,CRITICAL")
            @RequestParam(required = false) List<String> severities,
            @Parameter(description = "조회 시작 시각, ISO-8601", example = "2026-05-17T09:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "조회 종료 시각, ISO-8601", example = "2026-05-17T10:15:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "조회 개수. 1~1000으로 보정됨", example = "100")
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
