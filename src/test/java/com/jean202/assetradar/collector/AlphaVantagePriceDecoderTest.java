package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AlphaVantagePriceDecoderTest {
    private final AlphaVantagePriceDecoder decoder = new AlphaVantagePriceDecoder(new ObjectMapper());

    @Test
    void decodesGlobalQuoteToPrice() {
        String payload = """
                {
                  "Global Quote": {
                    "01. symbol": "AAPL",
                    "02. open": "198.4300",
                    "03. high": "199.6200",
                    "04. low": "196.8800",
                    "05. price": "198.5000",
                    "06. volume": "48237521",
                    "07. latest trading day": "2026-04-01",
                    "08. previous close": "199.5000",
                    "09. change": "-1.0000",
                    "10. change percent": "-0.5013%"
                  }
                }
                """;

        AlphaVantagePriceDecoder.AlphaVantageQuote result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.price()).isEqualByComparingTo("198.5000");
        assertThat(result.signedChangeRate()).isEqualByComparingTo("-0.005013");
    }

    @Test
    void returnsNullWhenGlobalQuoteIsMissing() {
        String payload = """
                {
                  "Note": "Thank you for using Alpha Vantage!"
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void returnsNullWhenPriceIsMissing() {
        String payload = """
                {
                  "Global Quote": {
                    "01. symbol": "AAPL"
                  }
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void handlesPositiveChangePercent() {
        String payload = """
                {
                  "Global Quote": {
                    "01. symbol": "NVDA",
                    "05. price": "890.2000",
                    "10. change percent": "4.2500%"
                  }
                }
                """;

        AlphaVantagePriceDecoder.AlphaVantageQuote result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("NVDA");
        assertThat(result.signedChangeRate()).isEqualByComparingTo("0.0425");
    }
}
