package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAlert;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "알림 이력 조회 응답")
public record AlertHistoryResponse(
        @Schema(description = "자산 심볼", example = "BTC", nullable = true)
        String symbol,
        @Schema(description = "데이터 소스", example = "UPBIT", nullable = true)
        String source,
        @Schema(description = "가격 기준 통화", example = "KRW", nullable = true)
        String quoteCurrency,
        @Schema(description = "조회 시작 시각", example = "2026-05-17T09:15:00Z", nullable = true)
        Instant from,
        @Schema(description = "조회 종료 시각", example = "2026-05-17T10:15:00Z", nullable = true)
        Instant to,
        @Schema(description = "응답 알림 수", example = "1")
        int count,
        @Schema(description = "알림 이력 목록")
        List<AssetAlert> alerts
) {
}
