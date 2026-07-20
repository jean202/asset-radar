package com.jean202.assetradar.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "수집된 자산 가격 이벤트")
public record AssetPrice(
        @Schema(description = "자산 심볼", example = "BTC")
        String symbol,
        @Schema(description = "표시 이름", example = "Bitcoin")
        String name,
        @Schema(description = "가격 기준 통화", example = "KRW")
        String quoteCurrency,
        @Schema(description = "데이터 소스", example = "UPBIT")
        String source,
        @Schema(description = "현재 가격", example = "137500000")
        BigDecimal price,
        @Schema(description = "소스가 제공하는 부호 있는 등락률", example = "0.018")
        BigDecimal signedChangeRate,
        @Schema(description = "가격 수집 시각", example = "2026-05-17T10:15:02Z")
        Instant collectedAt
) {
}
