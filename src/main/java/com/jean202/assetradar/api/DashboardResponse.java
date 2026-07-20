package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "대시보드 스냅샷 응답")
public record DashboardResponse(
        @Schema(description = "대시보드 상태", example = "live", allowableValues = {"live", "degraded", "warming-up"})
        String status,
        @Schema(description = "스냅샷 기준 최신 갱신 시각", example = "2026-05-17T10:15:02Z")
        Instant updatedAt,
        @Schema(description = "최신 캐시에 있는 자산 수", example = "2")
        int assetCount,
        @Schema(description = "PostgreSQL 가격 이력 row 수", example = "4821")
        long historyRowCount,
        @Schema(description = "가격 이력 테이블의 최신 수집 시각", example = "2026-05-17T10:15:02Z")
        Instant historyUpdatedAt,
        @Schema(description = "최신 자산 가격 목록")
        List<AssetPrice> assets,
        @Schema(description = "데이터 소스별 대시보드 그룹")
        List<DashboardSourceGroup> sourceGroups
) {
}
