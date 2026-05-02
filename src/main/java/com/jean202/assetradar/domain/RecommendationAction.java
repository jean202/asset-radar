package com.jean202.assetradar.domain;

/**
 * 자산에 대한 추천 액션을 나타내는 열거형
 * STRONG_BUY부터 STRONG_SELL까지 5단계 평가
 */
public enum RecommendationAction {
  STRONG_BUY("강력 매수", 5),
  BUY("매수", 4),
  HOLD("보유", 3),
  SELL("매도", 2),
  STRONG_SELL("강력 매도", 1);

  private final String label;
  private final int score;

  RecommendationAction(String label, int score) {
    this.label = label;
    this.score = score;
  }

  public String getLabel() {
    return label;
  }

  public int getScore() {
    return score;
  }

  public static RecommendationAction fromScore(double score) {
    if (score >= 4.5) return STRONG_BUY;
    if (score >= 3.5) return BUY;
    if (score >= 2.5) return HOLD;
    if (score >= 1.5) return SELL;
    return STRONG_SELL;
  }
}
