package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "자산 수익률 비교 응답")
public record CompareResponse(
        @Schema(description = "비교 시작 시각", example = "2026-04-17T10:15:00Z")
        Instant from,
        @Schema(description = "비교 종료 시각", example = "2026-05-17T10:15:00Z")
        Instant to,
        @Schema(description = "동일 금액 투자 결과 계산 기준 금액", example = "1000000", nullable = true)
        BigDecimal baseAmount,
        @Schema(description = "요청한 비교 대상 수", example = "2")
        int requestedCount,
        @Schema(description = "비교 가능한 데이터가 있었던 대상 수", example = "2")
        int count,
        @Schema(description = "projectedValue를 서로 비교해도 되는지 여부", example = "false")
        boolean projectedValueComparable,
        @Schema(description = "응답에 포함된 기준 통화 목록", example = "[\"KRW\", \"USD\"]")
        List<String> quoteCurrencies,
        @Schema(description = "projectedValue 비교 제한 안내", example = "projectedValue is only comparable when all assets share the same quoteCurrency", nullable = true)
        String projectedValueWarning,
        @Schema(description = "자산별 비교 결과")
        List<ComparedAssetResponse> comparisons
) {
}
