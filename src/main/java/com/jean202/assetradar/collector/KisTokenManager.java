package com.jean202.assetradar.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.StockKrCollectorProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class KisTokenManager {
    private static final Logger log = LoggerFactory.getLogger(KisTokenManager.class);
    private static final Duration REFRESH_MARGIN = Duration.ofHours(1);

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isExpiredOrExpiringSoon() {
            return Instant.now().isAfter(expiresAt.minus(REFRESH_MARGIN));
        }
    }

    private final StockKrCollectorProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedToken> cached = new AtomicReference<>();

    @Autowired
    public KisTokenManager(
            StockKrCollectorProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        this(properties, webClientBuilder.baseUrl(properties.getBaseUrl().toString()).build(), objectMapper);
    }

    KisTokenManager(
            StockKrCollectorProperties properties,
            WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<String> getToken() {
        return Mono.defer(() -> {
            if (properties.getAppKey().isBlank() || properties.getAppSecret().isBlank()) {
                return Mono.empty();
            }
            CachedToken current = cached.get();
            if (current != null && !current.isExpiredOrExpiringSoon()) {
                return Mono.just(current.accessToken());
            }
            return fetchToken();
        });
    }

    private Mono<String> fetchToken() {
        log.info("Fetching new KIS access token");
        return webClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", properties.getAppKey(),
                        "appsecret", properties.getAppSecret()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(payload -> {
                    try {
                        JsonNode root = objectMapper.readTree(payload);
                        String token = root.path("access_token").asText(null);
                        long expiresIn = root.path("expires_in").asLong(86400);
                        if (token == null || token.isBlank()) {
                            log.warn("KIS token response missing access_token: {}", payload);
                            return Mono.empty();
                        }
                        Instant expiresAt = Instant.now().plusSeconds(expiresIn);
                        cached.set(new CachedToken(token, expiresAt));
                        log.info("KIS access token acquired, expires in {}s", expiresIn);
                        return Mono.just(token);
                    } catch (Exception e) {
                        log.error("Failed to parse KIS token response", e);
                        return Mono.empty();
                    }
                });
    }
}
