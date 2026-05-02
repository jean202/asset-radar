package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetRecommendation;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 여러 자산 추천 결과를 반환하는 API 응답 DTO
 */
public record RecommendationsResponse(
    List<RecommendationResponse> recommendations,
    int totalCount,
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
