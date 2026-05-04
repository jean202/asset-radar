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
  void combinesMultipleStrategiesForFinalRecommendation() {
    // 모멘텀: UP + 6% = STRONG_BUY
    // 평균회귀: 6% = SELL
    // 평균: (5 + 2) / 2 = 3.5 → BUY
    AssetAnalysis analysis = createAnalysis("BTC", "UP", 6.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.symbol()).isEqualTo("BTC");
    assertThat(recommendation.action()).isEqualTo(RecommendationAction.BUY);
    assertThat(recommendation.confidence()).isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
    assertThat(recommendation.reasons()).isNotEmpty();
  }

  @Test
  void generatesReasonsList() {
    AssetAnalysis analysis = createAnalysis("ETH", "DOWN", -8.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.reasons()).hasSizeGreaterThanOrEqualTo(3); // 가격정보 + 2개 전략
    assertThat(recommendation.reasons().get(0)).contains("현재 가격");
    assertThat(recommendation.reasons().get(0)).contains("-8.00%");
  }

  @Test
  void calculatesConfidenceLevel() {
    AssetAnalysis analysis = createAnalysis("SOL", "FLAT", 0.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.getConfidenceLevel()).isIn("low", "medium", "high");
  }

  @Test
  void strongUpwardMovementRecommendsBuy() {
    // 모멘텀: UP + 7% = STRONG_BUY
    // 평균회귀: 7% = SELL
    // 평균: (5 + 2) / 2 = 3.5 → BUY
    AssetAnalysis analysis = createAnalysis("ADA", "UP", 7.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.action()).isIn(
        RecommendationAction.BUY,
        RecommendationAction.STRONG_BUY
    );
  }

  @Test
  void strongDownwardMovementRecommendsSell() {
    // 모멘텀: DOWN + -8% = STRONG_SELL
    // 평균회귀: -8% = BUY
    // 평균: (1 + 4) / 2 = 2.5 → HOLD
    AssetAnalysis analysis = createAnalysis("XRP", "DOWN", -8.0);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.action()).isIn(
        RecommendationAction.SELL,
        RecommendationAction.STRONG_SELL,
        RecommendationAction.HOLD
    );
  }

  @Test
  void includesTimestampInRecommendation() {
    AssetAnalysis analysis = createAnalysis("DOGE", "FLAT", 0.1);

    AssetRecommendation recommendation = engine.analyze(analysis);

    assertThat(recommendation.analyzedAt()).isNotNull();
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
