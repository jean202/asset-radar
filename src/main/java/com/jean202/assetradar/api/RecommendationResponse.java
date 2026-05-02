package com.jean202.assetradar.api;

import com.jean202.assetradar.domain.AssetRecommendation;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 자산 추천 API 응답 DTO
 */
public record RecommendationResponse(
    String symbol,
    String action,
    String actionLabel,
    double confidence,
    String confidenceLevel,
    List<String> reasons,
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
