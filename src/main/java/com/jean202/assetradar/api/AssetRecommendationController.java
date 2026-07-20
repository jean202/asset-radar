package com.jean202.assetradar.api;

import com.jean202.assetradar.analysis.recommendation.RecommendationEngine;
import com.jean202.assetradar.domain.AssetRecommendation;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Recommendations", description = "최신 분석 기반 포트폴리오 추천")
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
  @Operation(summary = "추천 목록 조회", description = "최신 분석 결과를 Momentum/Mean Reversion 전략으로 평가해 추천 액션을 반환합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "추천 목록",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = RecommendationsResponse.class),
          examples = @ExampleObject(name = "recommendations", value = OpenApiExamples.RECOMMENDATIONS)
      )
  )
  public Mono<RecommendationsResponse> getRecommendations(
      @Parameter(description = "데이터 소스", example = "UPBIT")
      @RequestParam(required = false) String source,
      @Parameter(description = "기준 통화", example = "KRW")
      @RequestParam(required = false) String quoteCurrency,
      @Parameter(description = "추천을 조회할 심볼 목록", example = "BTC,ETH")
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
  @Operation(summary = "단일 심볼 추천 조회", description = "특정 심볼의 최신 분석 결과를 기반으로 추천 액션과 근거를 반환합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "단일 추천",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = RecommendationResponse.class),
          examples = @ExampleObject(name = "recommendation", value = OpenApiExamples.RECOMMENDATION)
      )
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 path 또는 query param",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = ApiErrorResponse.class),
          examples = @ExampleObject(name = "invalid-request", value = OpenApiExamples.ERROR_INVALID_REQUEST)
      )
  )
  public Mono<RecommendationResponse> getRecommendationBySymbol(
      @Parameter(description = "자산 심볼", example = "BTC", required = true)
      @PathVariable String symbol,
      @Parameter(description = "데이터 소스", example = "UPBIT")
      @RequestParam(required = false) String source,
      @Parameter(description = "기준 통화", example = "KRW")
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
