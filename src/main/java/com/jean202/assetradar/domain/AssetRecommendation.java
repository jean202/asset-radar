package com.jean202.assetradar.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자산에 대한 추천 결과를 나타내는 불변 레코드
 * 각 자산에 대해 종합적인 추천 액션과 신뢰도(confidence), 근거를 포함
 */
public record AssetRecommendation(
    String symbol,
    RecommendationAction action,
    double confidence, // 0.0 ~ 1.0
    List<String> reasons,
    LocalDateTime analyzedAt) {

  public AssetRecommendation {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol cannot be blank");
    }
    if (action == null) {
      throw new IllegalArgumentException("action cannot be null");
    }
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
    }
    if (reasons == null || reasons.isEmpty()) {
      throw new IllegalArgumentException("reasons cannot be empty");
    }
    if (analyzedAt == null) {
      throw new IllegalArgumentException("analyzedAt cannot be null");
    }
  }

  public String getConfidenceLevel() {
    if (confidence >= 0.8) return "high";
    if (confidence >= 0.5) return "medium";
    return "low";
  }

  public String toPrettyString() {
    return String.format(
        "%s: %s (%.0f%% 신뢰도) - %s",
        symbol, action.getLabel(), confidence * 100, String.join(", ", reasons));
  }
}
