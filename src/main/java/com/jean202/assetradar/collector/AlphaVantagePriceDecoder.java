package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AlphaVantagePriceDecoder {
    private final ObjectMapper objectMapper;

    public AlphaVantagePriceDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AlphaVantageQuote decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null) {
                return null;
            }

            JsonNode quote = root.path("Global Quote");
            if (quote.isMissingNode() || quote.path("05. price").isMissingNode()) {
                return null;
            }

            String symbol = normalize(quote.path("01. symbol").asText(null));
            BigDecimal price = parseBigDecimal(quote.path("05. price").asText(null));
            BigDecimal changePercent = parseChangePercent(quote.path("10. change percent").asText(null));

            if (symbol.isEmpty() || price == null) {
                return null;
            }

            return new AlphaVantageQuote(symbol, price, changePercent, Instant.now());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode Alpha Vantage payload", exception);
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

    private BigDecimal parseChangePercent(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String cleaned = value.trim().replace("%", "");
        BigDecimal parsed = parseBigDecimal(cleaned);
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

    public record AlphaVantageQuote(
            String symbol,
            BigDecimal price,
            BigDecimal signedChangeRate,
            Instant collectedAt
    ) {
    }
}
