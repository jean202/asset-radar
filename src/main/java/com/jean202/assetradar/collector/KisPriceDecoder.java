package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KisPriceDecoder {
    private final ObjectMapper objectMapper;

    public KisPriceDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KisStockPrice decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null) {
                return null;
            }

            JsonNode output = root.path("output");
            if (output.isMissingNode() || output.path("stck_prpr").isMissingNode()) {
                return null;
            }

            String symbol = normalize(output.path("stck_shrn_iscd").asText(null));
            String name = output.path("hts_kor_isnm").asText("");
            BigDecimal price = parseBigDecimal(output.path("stck_prpr").asText(null));
            BigDecimal signedChangeRate = parseChangeRate(output.path("prdy_ctrt").asText(null));

            if (symbol.isEmpty() || price == null) {
                return null;
            }

            return new KisStockPrice(symbol, name, price, signedChangeRate, Instant.now());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode KIS payload", exception);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal parseChangeRate(String value) {
        BigDecimal parsed = parseBigDecimal(value);
        if (parsed == null) {
            return BigDecimal.ZERO;
        }
        return parsed.movePointLeft(2);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record KisStockPrice(
            String symbol,
            String name,
            BigDecimal price,
            BigDecimal signedChangeRate,
            Instant collectedAt
    ) {
    }
}
