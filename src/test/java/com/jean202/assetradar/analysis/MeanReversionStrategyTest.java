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
    // 극도로 낮은 가격 → 상승 예상
    AssetAnalysis analysis = createAnalysis("BTC", -12.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_BUY);
  }

  @Test
  void recommendsBuyOnStrongDowntrend() {
    // 강한 하락 → 약간의 매수
    AssetAnalysis analysis = createAnalysis("ETH", -7.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.BUY);
  }

  @Test
  void recommendsHoldOnModerateChange() {
    // 중간 범위 → 보유
    AssetAnalysis analysis = createAnalysis("SOL", 2.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.HOLD);
  }

  @Test
  void recommendsSellOnStrongUptrend() {
    // 강한 상승 → 약간의 매도 (하락 예상)
    AssetAnalysis analysis = createAnalysis("ADA", 7.5);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.SELL);
  }

  @Test
  void recommendsStrongSellOnExtremeUptrend() {
    // 극도로 높은 가격 → 강한 매도 신호
    AssetAnalysis analysis = createAnalysis("XRP", 12.0);

    RecommendationAction action = strategy.recommend(analysis);

    assertThat(action).isEqualTo(RecommendationAction.STRONG_SELL);
  }

  @Test
  void hasCorrectWeightAndName() {
    assertThat(strategy.getWeight()).isEqualTo(0.5);
    assertThat(strategy.getName()).isEqualTo("Mean Reversion Strategy");
  }

  private AssetAnalysis createAnalysis(String symbol, double changeRate) {
    return new AssetAnalysis(
        symbol,
        "quote",
        "source",
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(100 + (100 * changeRate / 100)),
        BigDecimal.valueOf(changeRate),
        BigDecimal.valueOf(changeRate),
        "UP",
        Instant.now()
    );
  }
}
