package com.jean202.assetradar.query;

import java.time.Instant;

public record DashboardHistoryMetrics(
        long historyRowCount,
        Instant latestCollectedAt
) {
}
