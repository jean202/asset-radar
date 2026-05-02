package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.recommendation.MomentumStrategy;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.RecommendationAction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MomentumStrategyTest {
  private MomentumStrategy strategy;

  @BeforeEach
  void setup() {
    strategy = new MomentumStrategy();
  }

  @Test
  void recommendsStrongBuyOnStrongUptrend() {
    AssetAnalysis analysis = createAnalysis("BTC", "UP", 6.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_BUY);
  }

  @Test
  void recommendsBuyOnMildUptrend() {
    AssetAnalysis analysis = createAnalysis("ETH", "UP", 2.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.BUY);
  }

  @Test
  void recommendsHoldOnFlat() {
    AssetAnalysis analysis = createAnalysis("SOL", "FLAT", 0.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.HOLD);
  }

  @Test
  void recommendsSellOnMildDowntrend() {
    AssetAnalysis analysis = createAnalysis("ADA", "DOWN", -2.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.SELL);
  }

  @Test
  void recommendsStrongSellOnStrongDowntrend() {
    AssetAnalysis analysis = createAnalysis("XRP", "DOWN", -7.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_SELL);
  }

  @Test
  void hasCorrectWeightAndName() {
    assertThat(strategy.getWeight()).isEqualTo(0.5);
    assertThat(strategy.getName()).isEqualTo("Momentum Strategy");
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
