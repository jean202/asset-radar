package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.collector.GoldApiPriceDecoder.GoldApiSpotPrice;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GoldApiPriceDecoderTest {
    private final GoldApiPriceDecoder decoder = new GoldApiPriceDecoder(new ObjectMapper());

    @Test
    void decodesGoldApiPayload() {
        String payload = """
                {
                  "currency": "USD",
                  "name": "Gold",
                  "price": 4674.399902,
                  "symbol": "XAU",
                  "updatedAt": "2026-04-01T04:28:11Z"
                }
                """;

        GoldApiSpotPrice spotPrice = decoder.decode(payload);

        assertThat(spotPrice).isNotNull();
        assertThat(spotPrice.symbol()).isEqualTo("XAU");
        assertThat(spotPrice.quoteCurrency()).isEqualTo("USD");
        assertThat(spotPrice.price()).hasToString("4674.399902");
        assertThat(spotPrice.updatedAt()).isEqualTo(Instant.parse("2026-04-01T04:28:11Z"));
    }
}
