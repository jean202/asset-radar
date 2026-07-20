package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 여러 자산 추천 결과를 반환하는 API 응답 DTO
 */
@Schema(description = "여러 자산 추천 목록 응답")
public record RecommendationsResponse(
    @Schema(description = "추천 목록")
    List<RecommendationResponse> recommendations,
    @Schema(description = "추천 수", example = "1")
    int totalCount,
    @Schema(description = "가장 최근 추천 산출 시각", example = "2026-05-17T10:15:03")
    LocalDateTime analyzedAt) {

  public static RecommendationsResponse from(List<AssetRecommendation> recommendations) {
    List<RecommendationResponse> responses = recommendations.stream()
        .map(RecommendationResponse::from)
        .toList();

    LocalDateTime latestTime = recommendations.isEmpty()
        ? LocalDateTime.now()
        : recommendations.get(0).analyzedAt();

    return new RecommendationsResponse(
        responses,
        responses.size(),
        latestTime
    );
  }
}
