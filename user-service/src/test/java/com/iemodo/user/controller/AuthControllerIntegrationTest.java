package com.iemodo.user.controller;

import com.iemodo.user.dto.LoginRequest;
import com.iemodo.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Auth Controller Integration Tests
 * Tests all authentication endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String TENANT_ID = "tenant_001";

    @Test
    void register_Success() {
        String email = "test" + System.currentTimeMillis() + "@example.com";
        
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("TestPassword123!");
        request.setDisplayName("Test User");

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.refreshToken").exists();
    }

    @Test
    void register_DuplicateEmail_ReturnsError() {
        // First register
        String email = "duplicate" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("TestPassword123!");
        request.setDisplayName("Test User");

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        // Try to register again with same email
        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void login_Success() {
        // First register a user
        String email = "login" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword("TestPassword123!");
        registerRequest.setDisplayName("Test User");

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk();

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("TestPassword123!");

        webTestClient.post()
                .uri("/uc/api/v1/auth/login")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.accessToken").exists()
                .jsonPath("$.data.refreshToken").exists();
    }

    @Test
    void login_InvalidCredentials_Returns401() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("WrongPassword123!");

        webTestClient.post()
                .uri("/uc/api/v1/auth/login")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void register_MissingTenantId_Returns400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void register_InvalidEmail_Returns400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("TestPassword123!");

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void register_WeakPassword_Returns400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test" + System.currentTimeMillis() + "@example.com");
        request.setPassword("123"); // Too weak

        webTestClient.post()
                .uri("/uc/api/v1/auth/register")
                .header("X-TenantID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void healthEndpoint_ReturnsStatus() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }
}
