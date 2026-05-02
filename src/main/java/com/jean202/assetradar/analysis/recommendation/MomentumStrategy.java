package com.jean202.assetradar.analysis.recommendation;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.RecommendationAction;

/**
 * 모멘텀 전략: 현재 가격 추세와 변화율을 기반으로 추천
 * 상승 추세에서는 매수, 하락 추세에서는 매도를 권장
 */
public class MomentumStrategy implements RecommendationStrategy {
  private static final double WEIGHT = 0.5;
  private static final String NAME = "Momentum Strategy";

  // 변화율 임계값 (%)
  private static final double STRONG_UP_THRESHOLD = 5.0;
  private static final double WEAK_UP_THRESHOLD = 0.5;
  private static final double WEAK_DOWN_THRESHOLD = -0.5;
  private static final double STRONG_DOWN_THRESHOLD = -5.0;

  @Override
  public RecommendationAction recommend(AssetAnalysis analysis) {
    double changeRate = analysis.changeRate().doubleValue();
    String movement = analysis.movement();

    // 강한 상승 추세 + 큰 상승률
    if ("UP".equals(movement) && changeRate >= STRONG_UP_THRESHOLD) {
      return RecommendationAction.STRONG_BUY;
    }

    // 상승 추세 + 약간의 상승률
    if ("UP".equals(movement) && changeRate >= WEAK_UP_THRESHOLD) {
      return RecommendationAction.BUY;
    }

    // 변화 없음
    if ("FLAT".equals(movement)) {
      return RecommendationAction.HOLD;
    }

    // 강한 하락 추세 + 큰 하락률 (먼저 확인)
    if ("DOWN".equals(movement) && changeRate <= STRONG_DOWN_THRESHOLD) {
      return RecommendationAction.STRONG_SELL;
    }

    // 하락 추세 + 약간의 하락률
    if ("DOWN".equals(movement) && changeRate <= WEAK_DOWN_THRESHOLD) {
      return RecommendationAction.SELL;
    }

    // 기본값: HOLD
    return RecommendationAction.HOLD;
  }

  @Override
  public double getWeight() {
    return WEIGHT;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
