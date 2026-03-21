package com.iemodo.user.controller;

import com.iemodo.common.response.Response;
import com.iemodo.user.dto.LoginRequest;
import com.iemodo.user.dto.RegisterRequest;
import com.iemodo.user.dto.TokenResponse;
import com.iemodo.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication endpoints: register, login, logout, token refresh.
 *
 * <p>All paths are whitelisted in the API Gateway (no JWT required to reach here).
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new local user (email + password).
     *
     * <pre>
     * POST /uc/api/v1/auth/register
     * X-TenantID: tenant_001
     * {"email":"...", "password":"...", "displayName":"..."}
     * </pre>
     */
    @PostMapping("/register")
    public Mono<Response<TokenResponse>> register(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid RegisterRequest request) {

        return authService.register(request, tenantId)
                .map(Response::success);
    }

    /**
     * Login with email and password.
     *
     * <pre>
     * POST /uc/api/v1/auth/login
     * X-TenantID: tenant_001
     * {"email":"...", "password":"..."}
     * </pre>
     */
    @PostMapping("/login")
    public Mono<Response<TokenResponse>> login(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid LoginRequest request) {

        return authService.login(request, tenantId)
                .map(Response::success);
    }

    /**
     * Logout the current user — blacklists the access token and revokes all
     * refresh tokens for the user.
     *
     * <pre>
     * POST /uc/api/v1/auth/logout
     * Authorization: Bearer <access_token>
     * X-User-ID: 12345   (set by gateway)
     * </pre>
     */
    @PostMapping("/logout")
    public Mono<Response<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-User-ID") Long userId) {

        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;

        return authService.logout(token, userId)
                .thenReturn(Response.<Void>success());
    }

    /**
     * Refresh access token using a valid refresh token.
     *
     * <pre>
     * POST /uc/api/v1/auth/refresh
     * X-TenantID: tenant_001
     * {"refreshToken":"..."}
     * </pre>
     */
    @PostMapping("/refresh")
    public Mono<Response<TokenResponse>> refresh(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody RefreshRequest request) {

        return authService.refresh(request.refreshToken(), tenantId)
                .map(Response::success);
    }

    // ─── Inner DTO ────────────────────────────────────────────────────────

    public record RefreshRequest(String refreshToken) {}
}
