package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetAlert;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최신 알림 조회 응답")
public record AlertsResponse(
        @Schema(description = "조회 조건의 데이터 소스", example = "UPBIT", nullable = true)
        String source,
        @Schema(description = "조회 조건의 기준 통화", example = "KRW", nullable = true)
        String quoteCurrency,
        @Schema(description = "응답 알림 수", example = "1")
        int count,
        @Schema(description = "최신 알림 목록")
        List<AssetAlert> alerts
) {
}
