package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "이동평균 계산 응답")
public record MovingAverageResponse(
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
        @Schema(description = "이동평균 타입", example = "SMA", allowableValues = {"SMA", "EMA"})
        String type,
        @Schema(description = "계산 윈도우 크기", example = "20")
        int window,
        @Schema(description = "응답 포인트 수", example = "1")
        int dataPoints,
        @Schema(description = "이동평균 시계열 포인트")
        List<MovingAveragePoint> points
) {
    @Schema(description = "이동평균 시계열 포인트")
    public record MovingAveragePoint(
            @Schema(description = "포인트 시각", example = "2026-05-17T10:15:00Z")
            Instant timestamp,
            @Schema(description = "해당 시점 가격", example = "137500000")
            BigDecimal price,
            @Schema(description = "계산된 이동평균", example = "136780000")
            BigDecimal movingAverage
    ) {
    }
}
