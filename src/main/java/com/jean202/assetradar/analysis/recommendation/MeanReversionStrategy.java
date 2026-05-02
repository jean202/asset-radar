package com.jean202.assetradar.analysis.recommendation;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.RecommendationAction;

/**
 * 평균회귀 전략: 극단적인 가격 변화가 평균으로 돌아올 것이라는 가설 기반
 * 극도로 높은 가격 변화 후에는 하락을 예상하고, 극도로 낮은 가격 변화 후에는 상승을 예상
 */
public class MeanReversionStrategy implements RecommendationStrategy {
  private static final double WEIGHT = 0.5;
  private static final String NAME = "Mean Reversion Strategy";

  // 변화율 임계값 (%)
  private static final double EXTREME_UP_THRESHOLD = 10.0;
  private static final double STRONG_UP_THRESHOLD = 5.0;
  private static final double EXTREME_DOWN_THRESHOLD = -10.0;
  private static final double STRONG_DOWN_THRESHOLD = -5.0;

  @Override
  public RecommendationAction recommend(AssetAnalysis analysis) {
    double changeRate = analysis.changeRate().doubleValue();

    // 극도로 높은 가격 -> 매도 신호 (하락 예상)
    if (changeRate >= EXTREME_UP_THRESHOLD) {
      return RecommendationAction.STRONG_SELL;
    }

    // 높은 가격 상승 -> 약간의 매도 신호
    if (changeRate >= STRONG_UP_THRESHOLD) {
      return RecommendationAction.SELL;
    }

    // 극도로 낮은 가격 -> 매수 신호 (상승 예상)
    if (changeRate <= EXTREME_DOWN_THRESHOLD) {
      return RecommendationAction.STRONG_BUY;
    }

    // 낮은 가격 하락 -> 약간의 매수 신호
    if (changeRate <= STRONG_DOWN_THRESHOLD) {
      return RecommendationAction.BUY;
    }

    // 중간 범위: HOLD
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
