package com.jean202.assetradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.recommendation.MeanReversionStrategy;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.RecommendationAction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeanReversionStrategyTest {
  private MeanReversionStrategy strategy;

  @BeforeEach
  void setup() {
    strategy = new MeanReversionStrategy();
  }

  @Test
  void recommendsStrongBuyOnExtremeDowntrend() {
    AssetAnalysis analysis = createAnalysis("BTC", "DOWN", -12.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_BUY);
  }

  @Test
  void recommendsBuyOnStrongDowntrend() {
    AssetAnalysis analysis = createAnalysis("ETH", "DOWN", -6.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.BUY);
  }

  @Test
  void recommendsHoldOnMediumRange() {
    AssetAnalysis analysis = createAnalysis("SOL", "UP", 3.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.HOLD);
  }

  @Test
  void recommendsSellOnStrongUptrend() {
    AssetAnalysis analysis = createAnalysis("ADA", "UP", 7.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.SELL);
  }

  @Test
  void recommendsStrongSellOnExtremeUptrend() {
    AssetAnalysis analysis = createAnalysis("XRP", "UP", 15.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_SELL);
  }

  @Test
  void hasCorrectWeightAndName() {
    assertThat(strategy.getWeight()).isEqualTo(0.5);
    assertThat(strategy.getName()).isEqualTo("Mean Reversion Strategy");
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
