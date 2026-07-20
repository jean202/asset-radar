package com.jean202.assetradar.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "가격 이벤트 간 변화 분석 결과")
public record AssetAnalysis(
        @Schema(description = "자산 심볼", example = "BTC")
        String symbol,
        @Schema(description = "가격 기준 통화", example = "KRW")
        String quoteCurrency,
        @Schema(description = "데이터 소스", example = "UPBIT")
        String source,
        @Schema(description = "현재 가격", example = "137500000")
        BigDecimal currentPrice,
        @Schema(description = "비교 기준 직전 가격", example = "136900000")
        BigDecimal previousPrice,
        @Schema(description = "현재 가격과 직전 가격의 차이", example = "600000")
        BigDecimal priceChange,
        @Schema(description = "가격 변화율", example = "0.00438276")
        BigDecimal changeRate,
        @Schema(description = "가격 이동 방향", example = "UP", allowableValues = {"UP", "DOWN", "FLAT"})
        String movement,
        @Schema(description = "분석 시각", example = "2026-05-17T10:15:03Z")
        Instant analyzedAt
) {
}
