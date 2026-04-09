package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FinnhubPriceDecoderTest {
    private final FinnhubPriceDecoder decoder = new FinnhubPriceDecoder(new ObjectMapper());

    @Test
    void decodesFinnhubQuotePayload() {
        String payload = """
                {
                  "c": 198.50,
                  "d": -1.00,
                  "dp": -0.5013,
                  "h": 199.62,
                  "l": 196.88,
                  "o": 198.43,
                  "pc": 199.50,
                  "t": 1774858530
                }
                """;

        FinnhubPriceDecoder.FinnhubQuote result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.price()).isEqualByComparingTo("198.50");
        assertThat(result.signedChangeRate()).isNegative();
    }

    @Test
    void returnsNullWhenCurrentPriceIsMissing() {
        String payload = """
                {
                  "dp": 0.5
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void returnsNullWhenCurrentPriceIsZero() {
        String payload = """
                {
                  "c": 0,
                  "pc": 100
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void handlesPositiveChange() {
        String payload = """
                {
                  "c": 210.00,
                  "pc": 200.00,
                  "t": 1774858530
                }
                """;

        FinnhubPriceDecoder.FinnhubQuote result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.price()).isEqualByComparingTo("210.00");
        assertThat(result.signedChangeRate()).isEqualByComparingTo("0.05");
    }
}
