package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class KisPriceDecoderTest {
    private final KisPriceDecoder decoder = new KisPriceDecoder(new ObjectMapper());

    @Test
    void decodesKisOutputToStockPrice() {
        String payload = """
                {
                  "output": {
                    "stck_shrn_iscd": "005930",
                    "hts_kor_isnm": "삼성전자",
                    "stck_prpr": "78500",
                    "prdy_ctrt": "1.23"
                  },
                  "rt_cd": "0",
                  "msg_cd": "MCA00000"
                }
                """;

        KisPriceDecoder.KisStockPrice result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("005930");
        assertThat(result.name()).isEqualTo("삼성전자");
        assertThat(result.price()).isEqualByComparingTo("78500");
        assertThat(result.signedChangeRate()).isEqualByComparingTo("0.0123");
    }

    @Test
    void returnsNullWhenOutputIsMissing() {
        String payload = """
                {
                  "rt_cd": "1",
                  "msg_cd": "EGW00000"
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void returnsNullWhenPriceIsMissing() {
        String payload = """
                {
                  "output": {
                    "stck_shrn_iscd": "005930",
                    "hts_kor_isnm": "삼성전자"
                  }
                }
                """;

        assertThat(decoder.decode(payload)).isNull();
    }

    @Test
    void handlesNegativeChangeRate() {
        String payload = """
                {
                  "output": {
                    "stck_shrn_iscd": "000660",
                    "hts_kor_isnm": "SK하이닉스",
                    "stck_prpr": "215000",
                    "prdy_ctrt": "-2.50"
                  }
                }
                """;

        KisPriceDecoder.KisStockPrice result = decoder.decode(payload);

        assertThat(result).isNotNull();
        assertThat(result.signedChangeRate()).isEqualByComparingTo("-0.0250");
    }
}
