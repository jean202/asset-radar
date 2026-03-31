package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.domain.AssetPrice;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class UpbitTickerDecoderTest {
    private final UpbitTickerDecoder decoder = new UpbitTickerDecoder(new ObjectMapper());

    @Test
    void createsSubscriptionPayloadForConfiguredSymbols() throws Exception {
        String payload = decoder.createSubscriptionPayload(java.util.List.of("KRW-BTC", "KRW-ETH"));
        var json = new ObjectMapper().readTree(payload);
        var codes = StreamSupport.stream(json.get(1).get("codes").spliterator(), false)
                .map(node -> node.asText())
                .toList();

        assertThat(json.size()).isEqualTo(2);
        assertThat(json.get(1).get("type").asText()).isEqualTo("ticker");
        assertThat(codes).containsExactly("KRW-BTC", "KRW-ETH");
    }

    @Test
    void decodesUpbitTickerPayload() {
        String payload = """
                {
                  "type": "ticker",
                  "code": "KRW-BTC",
                  "trade_price": 137500000.12,
                  "signed_change_rate": 0.023,
                  "timestamp": 1774858530000
                }
                """;

        AssetPrice assetPrice = decoder.decode(payload);

        assertThat(assetPrice).isNotNull();
        assertThat(assetPrice.symbol()).isEqualTo("BTC");
        assertThat(assetPrice.quoteCurrency()).isEqualTo("KRW");
        assertThat(assetPrice.source()).isEqualTo("UPBIT");
        assertThat(assetPrice.price()).hasToString("137500000.12");
        assertThat(assetPrice.signedChangeRate()).hasToString("0.023");
    }
}
