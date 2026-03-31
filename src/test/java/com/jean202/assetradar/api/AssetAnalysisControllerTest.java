package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetAnalysis;
import com.jean202.assetradar.query.AssetAnalysisHistoryQuery;
import com.jean202.assetradar.query.AssetAnalysisHistoryReader;
import com.jean202.assetradar.query.LatestAssetAnalysisQuery;
import com.jean202.assetradar.query.LatestAssetAnalysisReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AssetAnalysisControllerTest {
    @Test
    void latestReturnsLatestAnalysisEntries() {
        RecordingLatestReader latestReader = new RecordingLatestReader(List.of(
                analysis("BTC", "UP", "100", "90", "10", "0.11111111", "2026-03-31T00:00:01Z"),
                analysis("ETH", "DOWN", "50", "60", "-10", "-0.16666667", "2026-03-31T00:00:02Z")
        ));
        AssetAnalysisController controller = new AssetAnalysisController(latestReader, query -> Flux.empty());

        AnalysisResponse response = controller.latest("upbit", "krw", List.of("btc", "eth")).block();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.analyses()).extracting(AssetAnalysis::symbol).containsExactly("BTC", "ETH");
        assertThat(latestReader.lastQuery()).isEqualTo(new LatestAssetAnalysisQuery("UPBIT", "KRW", List.of("BTC", "ETH")));
    }

    @Test
    void historyReturnsBoundedAnalysisHistory() {
        RecordingHistoryReader historyReader = new RecordingHistoryReader(List.of(
                analysis("BTC", "UP", "100", "90", "10", "0.11111111", "2026-03-31T00:00:01Z")
        ));
        AssetAnalysisController controller = new AssetAnalysisController(query -> Flux.empty(), historyReader);

        AnalysisHistoryResponse response = controller.history(
                "btc",
                "upbit",
                "krw",
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                5000
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.symbol()).isEqualTo("BTC");
        assertThat(historyReader.lastQuery()).isEqualTo(new AssetAnalysisHistoryQuery(
                "BTC",
                "UPBIT",
                "KRW",
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                1000
        ));
    }

    private AssetAnalysis analysis(
            String symbol,
            String movement,
            String currentPrice,
            String previousPrice,
            String priceChange,
            String changeRate,
            String analyzedAt
    ) {
        return new AssetAnalysis(
                symbol,
                "KRW",
                "UPBIT",
                new BigDecimal(currentPrice),
                previousPrice == null ? null : new BigDecimal(previousPrice),
                new BigDecimal(priceChange),
                new BigDecimal(changeRate),
                movement,
                Instant.parse(analyzedAt)
        );
    }

    private static final class RecordingLatestReader implements LatestAssetAnalysisReader {
        private final List<AssetAnalysis> analyses;
        private LatestAssetAnalysisQuery lastQuery;

        private RecordingLatestReader(List<AssetAnalysis> analyses) {
            this.analyses = analyses;
        }

        @Override
        public Flux<AssetAnalysis> readLatest(LatestAssetAnalysisQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(analyses);
        }

        LatestAssetAnalysisQuery lastQuery() {
            return lastQuery;
        }
    }

    private static final class RecordingHistoryReader implements AssetAnalysisHistoryReader {
        private final List<AssetAnalysis> analyses;
        private AssetAnalysisHistoryQuery lastQuery;

        private RecordingHistoryReader(List<AssetAnalysis> analyses) {
            this.analyses = analyses;
        }

        @Override
        public Flux<AssetAnalysis> readHistory(AssetAnalysisHistoryQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(analyses);
        }

        AssetAnalysisHistoryQuery lastQuery() {
            return lastQuery;
        }
    }
}
