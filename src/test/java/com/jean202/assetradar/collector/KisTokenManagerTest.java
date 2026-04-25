package com.jean202.assetradar.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jean202.assetradar.config.StockKrCollectorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class KisTokenManagerTest {

    private static final String TOKEN_RESPONSE = """
            {
              "access_token": "eyJhbGciOiJIUzI1NiJ9.test",
              "token_type": "Bearer",
              "expires_in": 86400
            }
            """;

    private KisTokenManager managerWith(String appKey, String appSecret, String tokenResponse) {
        StockKrCollectorProperties props = new StockKrCollectorProperties();
        props.setAppKey(appKey);
        props.setAppSecret(appSecret);

        WebClient webClient = WebClient.builder()
                .exchangeFunction(req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(tokenResponse)
                        .build()))
                .build();

        return new KisTokenManager(props, webClient, new ObjectMapper());
    }

    @Test
    void returnsTokenWhenCredentialsPresent() {
        KisTokenManager manager = managerWith("my-key", "my-secret", TOKEN_RESPONSE);

        StepVerifier.create(manager.getToken())
                .assertNext(token -> assertThat(token).isEqualTo("eyJhbGciOiJIUzI1NiJ9.test"))
                .verifyComplete();
    }

    @Test
    void cachesTokenOnSecondCall() {
        int[] callCount = {0};
        StockKrCollectorProperties props = new StockKrCollectorProperties();
        props.setAppKey("my-key");
        props.setAppSecret("my-secret");

        WebClient webClient = WebClient.builder()
                .exchangeFunction(req -> {
                    callCount[0]++;
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body(TOKEN_RESPONSE)
                            .build());
                })
                .build();

        KisTokenManager manager = new KisTokenManager(props, webClient, new ObjectMapper());

        StepVerifier.create(manager.getToken().then(manager.getToken()))
                .assertNext(token -> {
                    assertThat(token).isEqualTo("eyJhbGciOiJIUzI1NiJ9.test");
                    assertThat(callCount[0]).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenAppKeyBlank() {
        KisTokenManager manager = managerWith("", "my-secret", TOKEN_RESPONSE);

        StepVerifier.create(manager.getToken())
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenAppSecretBlank() {
        KisTokenManager manager = managerWith("my-key", "", TOKEN_RESPONSE);

        StepVerifier.create(manager.getToken())
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenTokenMissingInResponse() {
        KisTokenManager manager = managerWith("my-key", "my-secret", "{\"token_type\":\"Bearer\"}");

        StepVerifier.create(manager.getToken())
                .verifyComplete();
    }
}
