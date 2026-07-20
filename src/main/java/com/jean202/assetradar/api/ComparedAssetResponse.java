package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "단일 자산 비교 결과")
public record ComparedAssetResponse(
        @Schema(description = "자산 심볼", example = "BTC")
        String symbol,
        @Schema(description = "데이터 소스", example = "UPBIT")
        String source,
        @Schema(description = "가격 기준 통화", example = "KRW")
        String quoteCurrency,
        @Schema(description = "비교 구간 시작 샘플 시각", example = "2026-04-17T10:15:00Z")
        Instant startedAt,
        @Schema(description = "비교 구간 종료 샘플 시각", example = "2026-05-17T10:15:00Z")
        Instant endedAt,
        @Schema(description = "비교에 사용한 샘플 수", example = "720")
        long sampleCount,
        @Schema(description = "비교 구간 시작 가격", example = "125000000")
        BigDecimal startPrice,
        @Schema(description = "비교 구간 종료 가격", example = "137500000")
        BigDecimal endPrice,
        @Schema(description = "가격 변화량", example = "12500000")
        BigDecimal priceChange,
        @Schema(description = "수익률", example = "0.10000000")
        BigDecimal returnRate,
        @Schema(description = "가격 이동 방향", example = "UP", allowableValues = {"UP", "DOWN", "FLAT"})
        String movement,
        @Schema(description = "baseAmount 기준 예상 평가액. 기준 통화가 섞이면 null", example = "1100000", nullable = true)
        BigDecimal projectedValue
) {
    public ComparedAssetResponse withoutProjectedValue() {
        return new ComparedAssetResponse(
                symbol,
                source,
                quoteCurrency,
                startedAt,
                endedAt,
                sampleCount,
                startPrice,
                endPrice,
                priceChange,
                returnRate,
                movement,
                null
        );
    }
}
