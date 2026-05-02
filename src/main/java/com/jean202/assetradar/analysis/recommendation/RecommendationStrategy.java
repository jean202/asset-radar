package com.jean202.assetradar.analysis.recommendation;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.RecommendationAction;

/**
 * 자산 추천 전략의 인터페이스
 * 다양한 분석 전략을 구현할 수 있도록 설계
 */
public interface RecommendationStrategy {
  /**
   * 주어진 자산 분석 데이터를 기반으로 추천 액션을 결정
   *
   * @param analysis 자산 분석 데이터
   * @return 추천 액션 (STRONG_BUY ~ STRONG_SELL)
   */
  RecommendationAction recommend(AssetAnalysis analysis);

  /**
   * 이 전략의 가중치를 반환 (0 ~ 1)
   *
   * @return 가중치
   */
  double getWeight();

  /**
   * 이 전략의 이름을 반환
   *
   * @return 전략 이름
   */
  String getName();

  /**
   * 추천 액션에 대한 신뢰도(confidence) 점수를 계산
   * score = action의 점수 / 최대값 (5)
   *
   * @param action 추천 액션
   * @return 신뢰도 (0 ~ 1)
   */
  default double calculateConfidence(RecommendationAction action) {
    return action.getScore() / 5.0;
  }
}
