package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class FinnhubPriceDecoder {
    private final ObjectMapper objectMapper;

    public FinnhubPriceDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FinnhubQuote decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null || root.path("c").isMissingNode()) {
                return null;
            }

            BigDecimal currentPrice = parseBigDecimal(root.path("c").asText(null));
            BigDecimal previousClose = parseBigDecimal(root.path("pc").asText(null));
            long timestamp = root.path("t").asLong(0);

            if (currentPrice == null || BigDecimal.ZERO.compareTo(currentPrice) == 0) {
                return null;
            }

            BigDecimal changeRate = BigDecimal.ZERO;
            if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
                changeRate = currentPrice.subtract(previousClose)
                        .divide(previousClose, MathContext.DECIMAL64);
            }

            Instant collectedAt = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now();
            return new FinnhubQuote(currentPrice, changeRate, collectedAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode Finnhub payload", exception);
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

    public record FinnhubQuote(
            BigDecimal price,
            BigDecimal signedChangeRate,
            Instant collectedAt
    ) {
    }
}
