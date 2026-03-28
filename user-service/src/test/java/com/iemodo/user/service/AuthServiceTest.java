package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.RefreshToken;
import com.iemodo.user.domain.User;
import com.iemodo.user.dto.LoginRequest;
import com.iemodo.user.dto.RegisterRequest;
import com.iemodo.user.dto.TokenResponse;
import com.iemodo.user.repository.RefreshTokenRepository;
import com.iemodo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, refreshTokenRepository, jwtService, passwordEncoder);

        // Default JWT mock behaviour
        when(jwtService.generateAccessToken(any(), any())).thenReturn("mock.access.token");
        when(jwtService.generateRawRefreshToken()).thenReturn("mock-raw-refresh-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(3600L);
        when(jwtService.refreshTokenTtlDays()).thenReturn(30L);
    }

    // ─── Register ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: should create user and return tokens when email not taken")
    void register_shouldSucceed_whenEmailNotTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("SecurePass1!");
        req.setDisplayName("Alice");

        User savedUser = User.builder()
                .id(1L).tenantId("tenant_001").email("alice@example.com")
                .displayName("Alice").oauthProvider("LOCAL")
                .status(1).createTime(Instant.now()).build();

        RefreshToken savedRt = RefreshToken.builder()
                .id(1L).userId(1L)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(false).build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(savedRt));

        StepVerifier.create(authService.register(req, "tenant_001"))
                .assertNext(resp -> {
                    assertThat(resp.getAccessToken()).isEqualTo("mock.access.token");
                    assertThat(resp.getUser().getEmail()).isEqualTo("alice@example.com");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("register: should fail with USER_ALREADY_EXISTS when email is taken")
    void register_shouldFail_whenEmailAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@example.com");
        req.setPassword("SecurePass1!");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(req, "tenant_001"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.USER_ALREADY_EXISTS)
                .verify();
    }

    // ─── Login ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: should return tokens for valid credentials")
    void login_shouldSucceed_withValidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bob@example.com");
        req.setPassword("Correct1!");

        String hash = passwordEncoder.encode("Correct1!");
        User user = User.builder()
                .id(2L).tenantId("tenant_001").email("bob@example.com")
                .passwordHash(hash).oauthProvider("LOCAL")
                .status(1).createTime(Instant.now()).build();

        RefreshToken rt = RefreshToken.builder()
                .id(2L).userId(2L)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(false).build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Mono.just(user));
        when(refreshTokenRepository.save(any())).thenReturn(Mono.just(rt));

        StepVerifier.create(authService.login(req, "tenant_001"))
                .assertNext(resp -> assertThat(resp.getAccessToken()).isEqualTo("mock.access.token"))
                .verifyComplete();
    }

    @Test
    @DisplayName("login: should fail with INVALID_CREDENTIALS for wrong password")
    void login_shouldFail_withWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bob@example.com");
        req.setPassword("WrongPass!");

        User user = User.builder()
                .id(2L).tenantId("tenant_001").email("bob@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPass1!"))
                .oauthProvider("LOCAL").status(1).build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Mono.just(user));

        StepVerifier.create(authService.login(req, "tenant_001"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.INVALID_CREDENTIALS)
                .verify();
    }

    @Test
    @DisplayName("login: should fail when user not found")
    void login_shouldFail_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("Any1!");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(req, "tenant_001"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.INVALID_CREDENTIALS)
                .verify();
    }
}
