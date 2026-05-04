package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetRecommendation;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AssetRecommendationControllerTest {

  @Test
  void getRecommendationsReturnsCombinedRecommendations() {
    // Mock reader with sample data
    LatestAssetAnalysisReader reader = (query) -> Flux.fromIterable(List.of(
        analysis("BTC", "UP", 5.5),
        analysis("ETH", "DOWN", -3.2)
    ));

    RecommendationEngine engine = new RecommendationEngine();
    AssetRecommendationController controller = new AssetRecommendationController(reader, engine);

    RecommendationsResponse response = controller.getRecommendations("upbit", "krw", List.of("BTC", "ETH")).block();

    assertThat(response).isNotNull();
    assertThat(response.totalCount()).isEqualTo(2);
    assertThat(response.recommendations()).hasSize(2);
    assertThat(response.recommendations())
        .extracting(RecommendationResponse::symbol)
        .containsExactly("BTC", "ETH");
  }

  @Test
  void getRecommendationsBySymbolReturnsSingleRecommendation() {
    LatestAssetAnalysisReader reader = (query) -> Flux.just(analysis("SOL", "FLAT", 0.1));

    RecommendationEngine engine = new RecommendationEngine();
    AssetRecommendationController controller = new AssetRecommendationController(reader, engine);

    RecommendationResponse response = controller.getRecommendationBySymbol("SOL", "upbit", "krw").block();

    assertThat(response).isNotNull();
    assertThat(response.symbol()).isEqualTo("SOL");
    assertThat(response.action()).isNotNull();
    assertThat(response.confidence()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
  }

  @Test
  void recommendationsIncludeAllRequiredFields() {
    LatestAssetAnalysisReader reader = (query) -> Flux.just(analysis("ADA", "UP", 7.0));

    RecommendationEngine engine = new RecommendationEngine();
    AssetRecommendationController controller = new AssetRecommendationController(reader, engine);

    RecommendationResponse response = controller.getRecommendationBySymbol("ADA", null, null).block();

    assertThat(response).isNotNull();
    assertThat(response.symbol()).isEqualTo("ADA");
    assertThat(response.action()).isNotNull();
    assertThat(response.actionLabel()).isNotNull();
    assertThat(response.confidence()).isGreaterThan(0.0);
    assertThat(response.confidenceLevel()).isIn("low", "medium", "high");
    assertThat(response.reasons()).isNotEmpty();
    assertThat(response.analyzedAt()).isNotNull();
  }

  @Test
  void emptyReaderReturnsEmptyRecommendations() {
    LatestAssetAnalysisReader reader = (query) -> Flux.empty();

    RecommendationEngine engine = new RecommendationEngine();
    AssetRecommendationController controller = new AssetRecommendationController(reader, engine);

    RecommendationsResponse response = controller.getRecommendations(null, null, null).block();

    assertThat(response).isNotNull();
    assertThat(response.totalCount()).isEqualTo(0);
    assertThat(response.recommendations()).isEmpty();
  }

  private AssetAnalysis analysis(String symbol, String movement, double changeRate) {
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
