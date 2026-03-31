package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetHistoryQuery;
import com.jean202.assetradar.query.AssetPriceHistoryReader;
import com.jean202.assetradar.query.LatestAssetPriceReader;
import com.jean202.assetradar.query.LatestAssetQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AssetQueryControllerTest {
    @Test
    void latestReturnsAssetsFromRedisReader() {
        RecordingLatestReader latestReader = new RecordingLatestReader(List.of(
                assetPrice("BTC", "KRW", "UPBIT", "101382000", "0.0099", "2026-03-31T00:00:00Z"),
                assetPrice("ETH", "KRW", "UPBIT", "3123000", "0.0354", "2026-03-31T00:00:01Z")
        ));
        AssetQueryController controller = new AssetQueryController(latestReader, query -> Flux.empty());

        LatestAssetsResponse response = controller.latest("upbit", "krw", List.of("btc", "eth")).block();

        assertThat(response).isNotNull();
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.assets()).extracting(AssetPrice::symbol).containsExactly("BTC", "ETH");
        assertThat(latestReader.lastQuery()).isEqualTo(new LatestAssetQuery("UPBIT", "KRW", List.of("BTC", "ETH")));
    }

    @Test
    void historyReturnsBoundedHistoryResponse() {
        RecordingHistoryReader historyReader = new RecordingHistoryReader(List.of(
                assetPrice("BTC", "KRW", "UPBIT", "101382000", "0.0099", "2026-03-31T00:00:00Z"),
                assetPrice("BTC", "KRW", "UPBIT", "101100000", "0.0085", "2026-03-30T23:59:00Z")
        ));
        AssetQueryController controller = new AssetQueryController(query -> Flux.empty(), historyReader);

        AssetHistoryResponse response = controller.history(
                "btc",
                "upbit",
                "krw",
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                5000
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.symbol()).isEqualTo("BTC");
        assertThat(response.source()).isEqualTo("UPBIT");
        assertThat(response.quoteCurrency()).isEqualTo("KRW");
        assertThat(response.count()).isEqualTo(2);
        assertThat(historyReader.lastQuery()).isEqualTo(new AssetHistoryQuery(
                "BTC",
                "UPBIT",
                "KRW",
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-31T00:00:00Z"),
                1000
        ));
    }

    private AssetPrice assetPrice(
            String symbol,
            String quoteCurrency,
            String source,
            String price,
            String signedChangeRate,
            String collectedAt
    ) {
        return new AssetPrice(
                symbol,
                quoteCurrency,
                source,
                new BigDecimal(price),
                new BigDecimal(signedChangeRate),
                Instant.parse(collectedAt)
        );
    }

    private static final class RecordingLatestReader implements LatestAssetPriceReader {
        private final List<AssetPrice> prices;
        private LatestAssetQuery lastQuery;

        private RecordingLatestReader(List<AssetPrice> prices) {
            this.prices = prices;
        }

        @Override
        public Flux<AssetPrice> readLatest(LatestAssetQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(prices);
        }

        LatestAssetQuery lastQuery() {
            return lastQuery;
        }
    }

    private static final class RecordingHistoryReader implements AssetPriceHistoryReader {
        private final List<AssetPrice> prices;
        private AssetHistoryQuery lastQuery;

        private RecordingHistoryReader(List<AssetPrice> prices) {
            this.prices = prices;
        }

        @Override
        public Flux<AssetPrice> readHistory(AssetHistoryQuery query) {
            this.lastQuery = query;
            return Flux.fromIterable(prices);
        }

        AssetHistoryQuery lastQuery() {
            return lastQuery;
        }
    }
}
