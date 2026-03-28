package com.iemodo.user.controller;

import com.iemodo.user.dto.LoginRequest;
import com.iemodo.user.dto.RegisterRequest;
import com.iemodo.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * User Controller Integration Tests
 * Tests user profile management endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UserControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String TENANT_ID = "tenant_001";
    private String accessToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        // Register and login to get token
        String email = "user" + System.currentTimeMillis() + "@example.com";
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("TestPassword123!");
        registerRequest.setDisplayName("Test User");

        // Register
        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk();

        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("TestPassword123!");

        var response = webTestClient.post()
                .uri("/uc/api/v1/auth/login")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.userId").exists()
                .returnResult();

        // Extract token and userId from response
        String responseBody = new String(response.getResponseBody());
        accessToken = extractJsonValue(responseBody, "accessToken");
        String userIdStr = extractJsonValue(responseBody, "userId");
        userId = userIdStr != null ? Long.parseLong(userIdStr) : 1L;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            // Try without quotes for numbers
            searchKey = "\"" + key + "\":";
            startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;
            startIndex += searchKey.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = json.indexOf("}", startIndex);
            return json.substring(startIndex, endIndex).replace("\"", "").trim();
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }

    @Test
    void getCurrentUser_Success() {
        webTestClient.get()
                .uri("/uc/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-ID", userId.toString())
                .header("X-TenantID", TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.email").exists();
    }

    @Test
    void getCurrentUser_MissingToken_Returns401() {
        webTestClient.get()
                .uri("/uc/api/v1/users/me")
                .header("X-User-ID", userId.toString())
                .header("X-TenantID", TENANT_ID)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void updateCurrentUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("Updated Name");
        request.setAvatarUrl("https://example.com/avatar.png");

        webTestClient.put()
                .uri("/uc/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-ID", userId.toString())
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.displayName").isEqualTo("Updated Name");
    }

    @Test
    void getUserById_Success() {
        webTestClient.get()
                .uri("/uc/api/v1/users/{userId}", userId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-ID", userId.toString())
                .header("X-TenantID", TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.id").isEqualTo(userId.intValue());
    }

    @Test
    void getUserById_NotFound_Returns404() {
        webTestClient.get()
                .uri("/uc/api/v1/users/999999")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-ID", userId.toString())
                .header("X-TenantID", TENANT_ID)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
