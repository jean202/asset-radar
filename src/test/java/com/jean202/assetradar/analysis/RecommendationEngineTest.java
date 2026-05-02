package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetRecommendation;
import com.jean202.assetradar.domain.RecommendationAction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationEngineTest {
  private RecommendationEngine engine;

  @BeforeEach
  void setup() {
    engine = new RecommendationEngine();
  }

  @Test
  void analyzesCombinesMultipleStrategies() {
    // 모멘텀 전략: STRONG_BUY (상승 추세, 큰 상승률)
    // 평균회귀 전략: HOLD (중간 범위)
    // 최종: BUY (두 전략의 가중 평균)
    AssetAnalysis analysis = createAnalysis("BTC", "UP", 6.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.symbol()).isEqualTo("BTC");
    assertThat(recommendation.action()).isIn(RecommendationAction.BUY, RecommendationAction.STRONG_BUY);
    assertThat(recommendation.confidence()).isBetween(0.0, 1.0);
    assertThat(recommendation.reasons()).isNotEmpty();
    assertThat(recommendation.analyzedAt()).isNotNull();
  }

  @Test
  void generatesMultipleReasons() {
    AssetAnalysis analysis = createAnalysis("ETH", "DOWN", -2.5);

    AssetRecommendation recommendation = engine.analyze(analysis);

    // 이유는 최소 3개 이상 (가격 정보 + 두 전략)
    assertThat(recommendation.reasons()).hasSizeGreaterThanOrEqualTo(3);
    assertThat(recommendation.reasons().get(0))
        .contains("현재 가격")
        .contains("변화율");
  }

  @Test
  void handlesBullishScenario() {
    // 모멘텀: STRONG_BUY, 평균회귀: SELL -> 가중평균으로 BUY
    AssetAnalysis analysis = createAnalysis("SOL", "UP", 12.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.confidence()).isGreaterThanOrEqualTo(0.4);
    assertThat(recommendation.action()).isNotEqualTo(RecommendationAction.STRONG_SELL);
  }

  @Test
  void handlesBearishScenario() {
    // 모멘텀: STRONG_SELL, 평균회귀: STRONG_BUY -> 가중평균으로 HOLD/SELL
    AssetAnalysis analysis = createAnalysis("ADA", "DOWN", -8.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.confidence()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  void validateRecommendationConstraints() {
    AssetAnalysis analysis = createAnalysis("XRP", "FLAT", 0.2);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.confidence()).isBetween(0.0, 1.0);
    assertThat(recommendation.symbol()).isEqualTo("XRP");
    assertThat(recommendation.action()).isNotNull();
    assertThat(recommendation.reasons()).isNotEmpty();
  }

  private AssetAnalysis createAnalysis(String symbol, String movement, double changeRate) {
    return new AssetAnalysis(
        symbol,
        "quote",
        "source",
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(100 + (100 * changeRate / 100)),
        BigDecimal.valueOf(changeRate),
        BigDecimal.valueOf(changeRate),
        movement,
        Instant.now()
    );
  }
}
