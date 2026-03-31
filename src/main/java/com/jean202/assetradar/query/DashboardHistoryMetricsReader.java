package com.jean202.assetradar.query;

import reactor.core.publisher.Mono;

public interface DashboardHistoryMetricsReader {
    Mono<DashboardHistoryMetrics> readMetrics();
}
