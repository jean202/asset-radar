package com.jean202.assetradar.config;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 추천 엔진 설정
 * RecommendationEngine을 Spring Bean으로 등록
 */
@Configuration
public class RecommendationEngineConfig {

  /**
   * RecommendationEngine 빈 생성
   * 기본 전략(Momentum, Mean Reversion)을 사용하는 엔진을 반환
   *
   * @return 추천 엔진 인스턴스
   */
  @Bean
  public RecommendationEngine recommendationEngine() {
    return new RecommendationEngine();
  }
}
