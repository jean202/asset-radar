package com.jean202.assetradar.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.domain.AssetPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UpbitTickerDecoder {
    private final ObjectMapper objectMapper;

    public UpbitTickerDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createSubscriptionPayload(List<String> symbols) {
        var root = objectMapper.createArrayNode();
        root.addObject().put("ticket", "asset-radar");

        var tickerRequest = root.addObject();
        tickerRequest.put("type", "ticker");
        var codes = tickerRequest.putArray("codes");
        symbols.forEach(codes::add);
        tickerRequest.put("isOnlyRealtime", true);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to create Upbit subscription payload", exception);
        }
    }

    public AssetPrice decode(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (root == null || root.path("code").isMissingNode() || root.path("trade_price").isMissingNode()) {
                return null;
            }

            String code = root.path("code").asText();
            String[] codeParts = code.split("-", 2);
            String quoteCurrency = codeParts.length == 2 ? codeParts[0] : code;
            String symbol = codeParts.length == 2 ? codeParts[1] : code;
            BigDecimal price = root.path("trade_price").decimalValue();
            BigDecimal signedChangeRate = root.path("signed_change_rate").isMissingNode()
                    ? BigDecimal.ZERO
                    : root.path("signed_change_rate").decimalValue();
            Instant collectedAt = root.path("timestamp").canConvertToLong()
                    ? Instant.ofEpochMilli(root.path("timestamp").asLong())
                    : Instant.now();

            return new AssetPrice(
                    symbol,
                    quoteCurrency,
                    "UPBIT",
                    price,
                    signedChangeRate,
                    collectedAt
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to decode Upbit ticker payload", exception);
        }
    }
}
