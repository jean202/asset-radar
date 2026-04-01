package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GoldApiPriceDecoder {
    private final ObjectMapper objectMapper;

    public GoldApiPriceDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GoldApiSpotPrice decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null || root.path("price").isMissingNode()) {
                return null;
            }

            String symbol = normalize(root.path("symbol").asText(null));
            String quoteCurrency = normalize(root.path("currency").asText(null));
            BigDecimal price = root.path("price").decimalValue();
            Instant updatedAt = parseInstant(root.path("updatedAt").asText(null));

            if (symbol.isEmpty() || quoteCurrency.isEmpty() || price == null) {
                return null;
            }

            return new GoldApiSpotPrice(symbol, quoteCurrency, price, updatedAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode gold-api payload", exception);
        }
    }

    private Instant parseInstant(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            return Instant.now();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record GoldApiSpotPrice(
            String symbol,
            String quoteCurrency,
            BigDecimal price,
            Instant updatedAt
    ) {
    }
}
