package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "변동성 계산 응답")
public record VolatilityResponse(
        @Schema(description = "자산 심볼", example = "BTC")
        String symbol,
        @Schema(description = "데이터 소스", example = "UPBIT", nullable = true)
        String source,
        @Schema(description = "가격 기준 통화", example = "KRW", nullable = true)
        String quoteCurrency,
        @Schema(description = "계산 시작 시각", example = "2026-04-17T10:15:00Z")
        Instant from,
        @Schema(description = "계산 종료 시각", example = "2026-05-17T10:15:00Z")
        Instant to,
        @Schema(description = "rolling volatility 윈도우 크기", example = "20")
        int window,
        @Schema(description = "응답 포인트 수", example = "1")
        int dataPoints,
        @Schema(description = "가장 최근 rolling volatility", example = "0.02134218", nullable = true)
        BigDecimal currentVolatility,
        @Schema(description = "연율화 변동성", example = "0.40782154", nullable = true)
        BigDecimal annualizedVolatility,
        @Schema(description = "rolling volatility 시계열 포인트")
        List<VolatilityPoint> points
) {
    @Schema(description = "변동성 시계열 포인트")
    public record VolatilityPoint(
            @Schema(description = "포인트 시각", example = "2026-05-17T10:15:00Z")
            Instant timestamp,
            @Schema(description = "rolling volatility", example = "0.02134218")
            BigDecimal volatility
    ) {
    }
}
