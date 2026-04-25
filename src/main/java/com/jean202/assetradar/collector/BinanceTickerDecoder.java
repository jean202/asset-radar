package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class BinanceTickerDecoder {
    private final ObjectMapper objectMapper;

    public BinanceTickerDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createSubscriptionPayload(List<String> symbols) {
        var root = objectMapper.createObjectNode();
        root.put("method", "SUBSCRIBE");
        var params = root.putArray("params");
        symbols.forEach(symbol -> params.add(symbol + "@ticker"));
        root.put("id", 1);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to create Binance subscription payload", exception);
        }
    }

    public AssetPrice decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null || root.path("e").isMissingNode() || !"24hrTicker".equals(root.path("e").asText())) {
                return null;
            }

            String rawSymbol = root.path("s").asText("").toUpperCase(Locale.ROOT);
            BigDecimal price = parseBigDecimal(root.path("c").asText(null));
            BigDecimal changeRate = parseBigDecimal(root.path("P").asText(null));
            long eventTime = root.path("E").asLong(0);

            if (rawSymbol.isEmpty() || price == null) {
                return null;
            }

            String symbol = extractSymbol(rawSymbol);
            String quoteCurrency = extractQuoteCurrency(rawSymbol);
            BigDecimal signedChangeRate = changeRate != null ? changeRate.movePointLeft(2) : BigDecimal.ZERO;
            Instant collectedAt = eventTime > 0 ? Instant.ofEpochMilli(eventTime) : Instant.now();

            return new AssetPrice(symbol, "", quoteCurrency, "BINANCE", price, signedChangeRate, collectedAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode Binance ticker payload", exception);
        }
    }

    private String extractSymbol(String rawSymbol) {
        if (rawSymbol.endsWith("USDT")) {
            return rawSymbol.substring(0, rawSymbol.length() - 4);
        }
        if (rawSymbol.endsWith("BTC")) {
            return rawSymbol.substring(0, rawSymbol.length() - 3);
        }
        return rawSymbol;
    }

    private String extractQuoteCurrency(String rawSymbol) {
        if (rawSymbol.endsWith("USDT")) {
            return "USDT";
        }
        if (rawSymbol.endsWith("BTC")) {
            return "BTC";
        }
        return "USDT";
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
}
