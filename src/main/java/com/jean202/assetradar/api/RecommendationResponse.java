package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 자산 추천 API 응답 DTO
 */
@Schema(description = "단일 자산 추천 응답")
public record RecommendationResponse(
    @Schema(description = "자산 심볼", example = "BTC")
    String symbol,
    @Schema(description = "추천 액션", example = "BUY", allowableValues = {"STRONG_BUY", "BUY", "HOLD", "SELL", "STRONG_SELL"})
    String action,
    @Schema(description = "화면 표시용 추천 액션 라벨", example = "매수")
    String actionLabel,
    @Schema(description = "추천 신뢰도. 0.0부터 1.0까지", example = "0.72")
    double confidence,
    @Schema(description = "신뢰도 구간", example = "medium", allowableValues = {"low", "medium", "high"})
    String confidenceLevel,
    @Schema(description = "추천 근거 목록", example = "[\"현재 가격: 137500000 (변화율: 0.44%, 움직임: UP)\"]")
    List<String> reasons,
    @Schema(description = "추천 산출 시각", example = "2026-05-17T10:15:03")
    LocalDateTime analyzedAt) {

  public static RecommendationResponse from(AssetRecommendation recommendation) {
    return new RecommendationResponse(
        recommendation.symbol(),
        recommendation.action().name(),
        recommendation.action().getLabel(),
        recommendation.confidence(),
        recommendation.getConfidenceLevel(),
        recommendation.reasons(),
        recommendation.analyzedAt()
    );
  }
}
