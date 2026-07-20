package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "가격 이력 요약 통계 응답")
public record SummaryResponse(
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
        @Schema(description = "계산에 사용한 가격 샘플 수", example = "720")
        long dataPoints,
        @Schema(description = "최저 가격", example = "125000000", nullable = true)
        BigDecimal min,
        @Schema(description = "최고 가격", example = "139000000", nullable = true)
        BigDecimal max,
        @Schema(description = "평균 가격", example = "132480000", nullable = true)
        BigDecimal mean,
        @Schema(description = "가격 표준편차", example = "2100000", nullable = true)
        BigDecimal stdDev,
        @Schema(description = "중앙값", example = "132700000", nullable = true)
        BigDecimal median,
        @Schema(description = "5 percentile", example = "126200000", nullable = true)
        BigDecimal p5,
        @Schema(description = "25 percentile", example = "130100000", nullable = true)
        BigDecimal p25,
        @Schema(description = "75 percentile", example = "135200000", nullable = true)
        BigDecimal p75,
        @Schema(description = "95 percentile", example = "138100000", nullable = true)
        BigDecimal p95,
        @Schema(description = "가장 최근 가격", example = "137500000", nullable = true)
        BigDecimal latestPrice,
        @Schema(description = "구간 수익률", example = "0.10000000", nullable = true)
        BigDecimal returnRate,
        @Schema(description = "구간 최대 낙폭", example = "0.04500000", nullable = true)
        BigDecimal maxDrawdown
) {
}
