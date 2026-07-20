package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "자산 간 수익률 상관관계 응답")
public record CorrelationResponse(
        @Schema(description = "계산 시작 시각", example = "2026-04-17T10:15:00Z")
        Instant from,
        @Schema(description = "계산 종료 시각", example = "2026-05-17T10:15:00Z")
        Instant to,
        @Schema(description = "요청 자산 수", example = "2")
        int assetCount,
        @Schema(description = "자산 쌍별 Pearson correlation 결과")
        List<CorrelationPair> pairs
) {
    @Schema(description = "두 자산의 상관관계 결과")
    public record CorrelationPair(
            @Schema(description = "첫 번째 자산 심볼", example = "BTC")
            String symbolA,
            @Schema(description = "첫 번째 자산 데이터 소스", example = "UPBIT")
            String sourceA,
            @Schema(description = "두 번째 자산 심볼", example = "ETH")
            String symbolB,
            @Schema(description = "두 번째 자산 데이터 소스", example = "UPBIT")
            String sourceB,
            @Schema(description = "Pearson correlation coefficient", example = "0.84321540")
            BigDecimal correlation,
            @Schema(description = "상관관계 계산에 사용한 겹치는 샘플 수", example = "720")
            long overlappingSamples
    ) {
    }
}
