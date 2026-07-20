package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "대시보드 데이터 소스 그룹")
public record DashboardSourceGroup(
        @Schema(description = "데이터 소스", example = "UPBIT")
        String source,
        @Schema(description = "화면 표시용 소스 이름", example = "업비트")
        String sourceDisplayName,
        @Schema(description = "자산 유형 코드", example = "COIN")
        String assetType,
        @Schema(description = "화면 표시용 자산 유형", example = "코인")
        String assetTypeLabel,
        @Schema(description = "해당 소스의 최신 갱신 시각", example = "2026-05-17T10:15:02Z")
        Instant updatedAt,
        @Schema(description = "소스별 stale threshold 초과 여부", example = "false")
        boolean stale,
        @Schema(description = "마지막 갱신 후 지난 시간, 초 단위", example = "1")
        long lastUpdatedAgeSeconds,
        @Schema(description = "화면 표시용 상대 갱신 시간", example = "1s ago")
        String lastUpdatedAgo,
        @Schema(description = "그룹 내 자산 수", example = "1")
        int assetCount,
        @Schema(description = "그룹 내 최신 자산 가격 목록")
        List<AssetPrice> assets
) {
}
