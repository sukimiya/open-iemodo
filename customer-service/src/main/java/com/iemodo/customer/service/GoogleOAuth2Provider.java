package com.iemodo.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * Google OAuth2 provider implementation.
 *
 * <p>Exchanges an authorization code for an id_token via Google's token endpoint,
 * then decodes the JWT id_token to extract user profile information.
 */
@Slf4j
@Component
public class GoogleOAuth2Provider implements OAuth2Service {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "openid profile email";

    private final String clientId;
    private final String clientSecret;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GoogleOAuth2Provider(
            @Value("${iemodo.customer.oauth2.google.client-id}") String clientId,
            @Value("${iemodo.customer.oauth2.google.client-secret}") String clientSecret,
            ObjectMapper objectMapper) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClient = WebClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "GOOGLE";
    }

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        return AUTH_URL + "?"
                + "client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + SCOPE
                + "&state=" + state
                + "&access_type=offline"
                + "&prompt=consent";
    }

    @Override
    public Mono<OAuth2UserInfo> exchangeCodeForUser(String code, String redirectUri) {
        return webClient.post()
                .uri(TOKEN_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue("code=" + code
                        + "&client_id=" + clientId
                        + "&client_secret=" + clientSecret
                        + "&redirect_uri=" + redirectUri
                        + "&grant_type=authorization_code")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        String idToken = root.get("id_token").asText();
                        OAuth2UserInfo userInfo = decodeIdToken(idToken);
                        log.debug("Google OAuth2 login: sub={} email={}", userInfo.subject(), userInfo.email());
                        return Mono.just(userInfo);
                    } catch (Exception e) {
                        log.error("Failed to parse Google token response: {}", e.getMessage());
                        return Mono.error(new RuntimeException("Failed to parse Google token response", e));
                    }
                });
    }

    /**
     * Decode a Google-signed id_token JWT (base64-encoded JSON payload part only).
     * We do NOT verify the signature here — Google's token endpoint already validated the code.
     */
    private OAuth2UserInfo decodeIdToken(String idToken) throws Exception {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid id_token format");
        }

        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        JsonNode claims = objectMapper.readTree(payload);

        return new OAuth2UserInfo(
                "GOOGLE",
                claims.get("sub").asText(),
                claims.has("email") && !claims.get("email").isNull()
                        ? claims.get("email").asText() : null,
                claims.has("name") && !claims.get("name").isNull()
                        ? claims.get("name").asText() : null,
                claims.has("picture") && !claims.get("picture").isNull()
                        ? claims.get("picture").asText() : null
        );
    }
}
