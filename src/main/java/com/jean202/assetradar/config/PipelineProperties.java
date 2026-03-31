package com.jean202.assetradar.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-radar.pipeline")
public class PipelineProperties {
    private String kafkaTopic = "asset.coin.realtime";
    private String analysisKafkaTopic = "asset.coin.analysis";
    private String redisKeyPrefix = "asset:latest";
    private String analysisRedisKeyPrefix = "analysis:latest";
    private String alertRedisKeyPrefix = "alert:latest";
    private Duration redisTtl = Duration.ofHours(6);

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public String getAnalysisKafkaTopic() {
        return analysisKafkaTopic;
    }

    public void setAnalysisKafkaTopic(String analysisKafkaTopic) {
        this.analysisKafkaTopic = analysisKafkaTopic;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public String getAnalysisRedisKeyPrefix() {
        return analysisRedisKeyPrefix;
    }

    public void setAnalysisRedisKeyPrefix(String analysisRedisKeyPrefix) {
        this.analysisRedisKeyPrefix = analysisRedisKeyPrefix;
    }

    public String getAlertRedisKeyPrefix() {
        return alertRedisKeyPrefix;
    }

    public void setAlertRedisKeyPrefix(String alertRedisKeyPrefix) {
        this.alertRedisKeyPrefix = alertRedisKeyPrefix;
    }

    public Duration getRedisTtl() {
        return redisTtl;
    }

    public void setRedisTtl(Duration redisTtl) {
        this.redisTtl = redisTtl;
    }
}
