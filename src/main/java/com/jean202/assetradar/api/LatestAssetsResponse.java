package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetPrice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최신 자산 가격 조회 응답")
public record LatestAssetsResponse(
        @Schema(description = "조회 조건의 데이터 소스", example = "UPBIT", nullable = true)
        String source,
        @Schema(description = "조회 조건의 기준 통화", example = "KRW", nullable = true)
        String quoteCurrency,
        @Schema(description = "응답 자산 수", example = "2")
        int count,
        @Schema(description = "최신 자산 가격 목록")
        List<AssetPrice> assets
) {
}
