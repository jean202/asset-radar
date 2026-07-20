package com.jean202.assetradar.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "가격 변화 알림 이벤트")
public record AssetAlert(
        @Schema(description = "자산 심볼", example = "BTC")
        String symbol,
        @Schema(description = "가격 기준 통화", example = "KRW")
        String quoteCurrency,
        @Schema(description = "데이터 소스", example = "UPBIT")
        String source,
        @Schema(description = "알림 유형", example = "PRICE_CHANGE")
        String alertType,
        @Schema(description = "알림 심각도", example = "WARN", allowableValues = {"INFO", "WARN", "CRITICAL"})
        String severity,
        @Schema(description = "가격 이동 방향", example = "UP", allowableValues = {"UP", "DOWN", "FLAT"})
        String movement,
        @Schema(description = "현재 가격", example = "137500000")
        BigDecimal currentPrice,
        @Schema(description = "비교 기준 가격", example = "134500000")
        BigDecimal previousPrice,
        @Schema(description = "현재 가격과 기준 가격의 차이", example = "3000000")
        BigDecimal priceChange,
        @Schema(description = "가격 변화율", example = "0.02230483")
        BigDecimal changeRate,
        @Schema(description = "알림 발생 기준 변화율", example = "0.01")
        BigDecimal thresholdRate,
        @Schema(description = "비교 기준 시각", example = "2026-05-17T10:14:00Z")
        Instant baselineAt,
        @Schema(description = "알림 평가 윈도우 초 단위", example = "60")
        Long windowSeconds,
        @Schema(description = "알림 메시지", example = "BTC moved 2.23% in 60 seconds")
        String message,
        @Schema(description = "알림 발생 시각", example = "2026-05-17T10:15:03Z")
        Instant alertedAt
) {
}
