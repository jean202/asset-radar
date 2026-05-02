package com.jean202.assetradar.analysis.recommendation;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.domain.AssetRecommendation;
import com.jean202.assetradar.domain.RecommendationAction;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 여러 추천 전략을 조합하여 최종 추천을 생성하는 엔진
 * 각 전략의 가중치를 반영하여 정합적인 추천 액션을 결정
 */
public class RecommendationEngine {
  private final List<RecommendationStrategy> strategies;

  public RecommendationEngine() {
    // 기본 전략 등록
    this.strategies = Arrays.asList(
        new MomentumStrategy(),
        new MeanReversionStrategy()
    );
  }

  public RecommendationEngine(List<RecommendationStrategy> customStrategies) {
    this.strategies = customStrategies;
  }

  /**
   * 주어진 자산 분석에 대해 종합적인 추천을 생성
   *
   * @param analysis 자산 분석 데이터
   * @return 추천 결과
   */
  public AssetRecommendation analyze(AssetAnalysis analysis) {
    // 각 전략의 추천과 신뢰도 계산
    List<StrategyVote> votes = new ArrayList<>();
    for (RecommendationStrategy strategy : strategies) {
      RecommendationAction action = strategy.recommend(analysis);
      double confidence = strategy.calculateConfidence(action);
      double weight = strategy.getWeight();
      votes.add(new StrategyVote(strategy.getName(), action, confidence, weight));
    }

    // 가중치를 적용한 최종 액션 결정
    RecommendationAction finalAction = determineFinalAction(votes);
    double finalConfidence = calculateWeightedConfidence(votes);
    List<String> reasons = generateReasons(votes, analysis);

    return new AssetRecommendation(
        analysis.symbol(),
        finalAction,
        finalConfidence,
        reasons,
        LocalDateTime.now()
    );
  }

  /**
   * 전략들의 투표를 기반으로 최종 액션 결정
   * 가중치를 고려하여 점수를 계산하고 점수에 기반한 액션 결정
   */
  private RecommendationAction determineFinalAction(List<StrategyVote> votes) {
    double totalScore = 0.0;
    double totalWeight = 0.0;

    for (StrategyVote vote : votes) {
      double score = vote.action.getScore() * vote.weight;
      totalScore += score;
      totalWeight += vote.weight;
    }

    double averageScore = totalWeight > 0 ? totalScore / totalWeight : 3;
    return RecommendationAction.fromScore(averageScore);
  }

  /**
   * 가중 평균을 사용한 최종 신뢰도 계산
   */
  private double calculateWeightedConfidence(List<StrategyVote> votes) {
    double totalConfidence = 0.0;
    double totalWeight = 0.0;

    for (StrategyVote vote : votes) {
      totalConfidence += vote.confidence * vote.weight;
      totalWeight += vote.weight;
    }

    return totalWeight > 0 ? totalConfidence / totalWeight : 0.5;
  }

  /**
   * 추천의 근거를 생성
   */
  private List<String> generateReasons(List<StrategyVote> votes, AssetAnalysis analysis) {
    List<String> reasons = new ArrayList<>();

    // 현재 가격 변화 정보
    reasons.add(String.format(
        "현재 가격: %.2f (변화율: %.2f%%, 움직임: %s)",
        analysis.currentPrice().doubleValue(),
        analysis.changeRate().doubleValue(),
        analysis.movement()
    ));

    // 각 전략의 투표 정보
    for (StrategyVote vote : votes) {
      reasons.add(String.format(
          "%s: %s (신뢰도: %.0f%%)",
          vote.strategyName,
          vote.action.getLabel(),
          vote.confidence * 100
      ));
    }

    return reasons;
  }

  /**
   * 단일 전략의 투표 정보
   */
  private static class StrategyVote {
    final String strategyName;
    final RecommendationAction action;
    final double confidence;
    final double weight;

    StrategyVote(String strategyName, RecommendationAction action, double confidence, double weight) {
      this.strategyName = strategyName;
      this.action = action;
      this.confidence = confidence;
      this.weight = weight;
    }
  }
}
