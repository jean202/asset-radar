package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.domain.AssetPrice;
import java.util.List;
import org.junit.jupiter.api.Test;

class BinanceTickerDecoderTest {
    private final BinanceTickerDecoder decoder = new BinanceTickerDecoder(new ObjectMapper());

    @Test
    void createsSubscriptionPayloadForConfiguredSymbols() throws Exception {
        String payload = decoder.createSubscriptionPayload(List.of("btcusdt", "ethusdt"));
        var json = new ObjectMapper().readTree(payload);

        assertThat(json.get("method").asText()).isEqualTo("SUBSCRIBE");
        var params = json.get("params");
        assertThat(params.get(0).asText()).isEqualTo("btcusdt@ticker");
        assertThat(params.get(1).asText()).isEqualTo("ethusdt@ticker");
    }

    @Test
    void decodesBinanceTickerPayload() {
        String payload = """
                {
                  "e": "24hrTicker",
                  "E": 1774858530000,
                  "s": "BTCUSDT",
                  "c": "67523.45",
                  "P": "2.35"
                }
                """;

        AssetPrice result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("BTC");
        assertThat(result.quoteCurrency()).isEqualTo("USDT");
        assertThat(result.source()).isEqualTo("BINANCE");
        assertThat(result.price()).isEqualByComparingTo("67523.45");
        assertThat(result.signedChangeRate()).isEqualByComparingTo("0.0235");
    }

    @Test
    void returnsNullForNonTickerEvent() {
        String payload = """
                {
                  "result": null,
                  "id": 1
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void returnsNullWhenPriceIsMissing() {
        String payload = """
                {
                  "e": "24hrTicker",
                  "s": "BTCUSDT"
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void handlesEthBtcPair() {
        String payload = """
                {
                  "e": "24hrTicker",
                  "E": 1774858530000,
                  "s": "ETHBTC",
                  "c": "0.04523",
                  "P": "-1.20"
                }
                """;

        AssetPrice result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("ETH");
        assertThat(result.quoteCurrency()).isEqualTo("BTC");
    }
}
