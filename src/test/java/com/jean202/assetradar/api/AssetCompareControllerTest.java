package com.jean202.assetradar.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jean202.assetradar.analysis.AssetComparator;
import com.jean202.assetradar.domain.AssetPrice;
import com.jean202.assetradar.query.AssetCompareQuery;
import com.jean202.assetradar.query.AssetCompareWindow;
import com.jean202.assetradar.query.AssetCompareWindowReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AssetCompareControllerTest {
    private RecordingCompareWindowReader compareWindowReader;
    private AssetCompareController controller;

    @BeforeEach
    void setUp() {
        compareWindowReader = new RecordingCompareWindowReader();
        controller = new AssetCompareController(
                compareWindowReader,
                new AssetComparator(),
                Clock.fixed(Instant.parse("2026-03-31T00:00:00Z"), java.time.ZoneOffset.UTC)
        );
    }

    @Test
    void comparesAssetsAcrossSourcesAndSortsByReturnRate() {
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                window("BTC", "KRW", "UPBIT", "100", "120")
        );
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD"),
                window("XAU", "USD", "GOLDAPI", "200", "210")
        );

        CompareResponse response = controller.compare(
                List.of("upbit:krw:btc,goldapi:usd:xau"),
                null,
                null,
                "30d",
                null,
                null,
                new BigDecimal("1000000")
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.projectedValueComparable()).isFalse();
        assertThat(response.quoteCurrencies()).containsExactly("KRW", "USD");
        assertThat(response.projectedValueWarning())
                .isEqualTo("projectedValue is only comparable when all assets share the same quoteCurrency");
        assertThat(response.comparisons())
                .extracting(comparedAsset -> "%s:%s".formatted(comparedAsset.source(), comparedAsset.symbol()))
                .containsExactly("UPBIT:BTC", "GOLDAPI:XAU");
        assertThat(response.comparisons())
                .extracting(ComparedAssetResponse::returnRate)
                .containsExactly(new BigDecimal("0.20000000"), new BigDecimal("0.05000000"));
        assertThat(response.comparisons())
                .extracting(ComparedAssetResponse::projectedValue)
                .containsOnlyNulls();
        assertThat(compareWindowReader.requests).containsExactly(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                new AssetCompareQuery.AssetSpec("XAU", "GOLDAPI", "USD")
        );
    }

    @Test
    void omitsAssetsWithoutHistoryWindow() {
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                window("BTC", "KRW", "UPBIT", "100", "120")
        );

        CompareResponse response = controller.compare(
                List.of("upbit:krw:btc,goldapi:usd:xau"),
                null,
                null,
                "30d",
                null,
                null,
                new BigDecimal("1000000")
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.projectedValueComparable()).isTrue();
        assertThat(response.quoteCurrencies()).containsExactly("KRW");
        assertThat(response.projectedValueWarning()).isNull();
        assertThat(response.comparisons())
                .extracting(ComparedAssetResponse::symbol)
                .containsExactly("BTC");
        assertThat(response.comparisons().get(0).projectedValue()).isEqualByComparingTo("1200000.00");
    }

    @Test
    void keepsProjectedValueWhenAllAssetsShareQuoteCurrency() {
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("BTC", "UPBIT", "KRW"),
                window("BTC", "KRW", "UPBIT", "100", "120")
        );
        compareWindowReader.windows.put(
                new AssetCompareQuery.AssetSpec("ETH", "UPBIT", "KRW"),
                window("ETH", "KRW", "UPBIT", "100", "110")
        );

        CompareResponse response = controller.compare(
                List.of("upbit:krw:btc,upbit:krw:eth"),
                null,
                null,
                "30d",
                null,
                null,
                new BigDecimal("1000000")
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.projectedValueComparable()).isTrue();
        assertThat(response.quoteCurrencies()).containsExactly("KRW");
        assertThat(response.projectedValueWarning()).isNull();
        assertThat(response.comparisons())
                .extracting(comparedAsset -> comparedAsset.projectedValue().toPlainString())
                .containsExactly("1200000.00", "1100000.00");
    }

    private AssetCompareWindow window(
            String symbol,
            String quoteCurrency,
            String source,
            String startPrice,
            String endPrice
    ) {
        return new AssetCompareWindow(
                new AssetCompareQuery.AssetSpec(symbol, source, quoteCurrency),
                new AssetPrice(
                        symbol,
                        quoteCurrency,
                        source,
                        new BigDecimal(startPrice),
                        BigDecimal.ZERO,
                        Instant.parse("2026-03-01T00:00:00Z")
                ),
                new AssetPrice(
                        symbol,
                        quoteCurrency,
                        source,
                        new BigDecimal(endPrice),
                        BigDecimal.ZERO,
                        Instant.parse("2026-03-31T00:00:00Z")
                ),
                2
        );
    }

    private static final class RecordingCompareWindowReader implements AssetCompareWindowReader {
        private final Map<AssetCompareQuery.AssetSpec, AssetCompareWindow> windows = new HashMap<>();
        private final List<AssetCompareQuery.AssetSpec> requests = new ArrayList<>();

        @Override
        public Mono<AssetCompareWindow> readWindow(AssetCompareQuery.AssetSpec asset, Instant from, Instant to) {
            requests.add(asset);
            return Mono.justOrEmpty(windows.get(asset));
        }
    }
}
