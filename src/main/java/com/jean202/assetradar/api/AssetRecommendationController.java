package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import com.jean202.assetradar.domain.AssetRecommendation;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 자산 추천 API 엔드포인트
 * RecommendationEngine을 사용하여 최신 분석에 기반한 추천을 제공
 */
@RestController
@RequestMapping("/api/recommendations")
public class AssetRecommendationController {
  private final LatestAssetAnalysisReader latestAssetAnalysisReader;
  private final RecommendationEngine recommendationEngine;

  public AssetRecommendationController(
      LatestAssetAnalysisReader latestAssetAnalysisReader,
      RecommendationEngine recommendationEngine) {
    this.latestAssetAnalysisReader = latestAssetAnalysisReader;
    this.recommendationEngine = recommendationEngine;
  }

  /**
   * 지정된 자산들의 추천을 조회
   *
   * @param source 데이터 출처 (선택사항)
   * @param quoteCurrency 기준 통화 (선택사항)
   * @param symbols 자산 심볼 목록 (선택사항)
   * @return 추천 결과 응답
   */
  @GetMapping
  public Mono<RecommendationsResponse> getRecommendations(
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String quoteCurrency,
      @RequestParam(required = false) List<String> symbols) {

    LatestAssetAnalysisQuery query = new LatestAssetAnalysisQuery(source, quoteCurrency, symbols);

    return latestAssetAnalysisReader
        .readLatest(query)
        .map(recommendationEngine::analyze)
        .collectList()
        .map(RecommendationsResponse::from);
  }

  /**
   * 특정 심볼의 추천을 조회
   *
   * @param symbol 자산 심볼
   * @param source 데이터 출처 (선택사항)
   * @param quoteCurrency 기준 통화 (선택사항)
   * @return 추천 결과 응답
   */
  @GetMapping("/symbol/{symbol}")
  public Mono<RecommendationResponse> getRecommendationBySymbol(
      @RequestParam String symbol,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String quoteCurrency) {

    LatestAssetAnalysisQuery query =
        new LatestAssetAnalysisQuery(source, quoteCurrency, List.of(symbol));

    return latestAssetAnalysisReader
        .readLatest(query)
        .next()
        .map(recommendationEngine::analyze)
        .map(RecommendationResponse::from);
  }
}
