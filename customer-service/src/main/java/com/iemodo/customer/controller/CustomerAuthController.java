package com.iemodo.customer.controller;

import com.iemodo.common.response.Response;
import com.iemodo.customer.dto.*;
import com.iemodo.customer.service.CustomerAuthService;
import com.iemodo.customer.service.OAuth2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/cc/api/v1/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService authService;
    private final List<OAuth2Service> oauth2Providers;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final com.iemodo.customer.service.SmsOtpService smsOtpService;

    // ─── SMS / Phone OTP ───────────────────────────────────────────────────

    @PostMapping("/sms/send")
    public Mono<Response<Void>> sendSmsOtp(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid SmsSendRequest request,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "0.0.0.0") String ip) {
        return smsOtpService.generateAndSend(request.getPhoneNumber(), tenantId, ip)
                .thenReturn(Response.<Void>success());
    }

    @PostMapping("/sms/login")
    public Mono<Response<TokenResponse>> smsLogin(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid SmsLoginRequest request,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "0.0.0.0") String ip) {
        return authService.smsLogin(request.getPhoneNumber(), request.getOtpCode(), tenantId, ip)
                .map(Response::success);
    }

    // ─── Email + Password ─────────────────────────────────────────────────

    @PostMapping("/email/register")
    public Mono<Response<TokenResponse>> emailRegister(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid EmailRegisterRequest request) {
        return authService.emailRegister(request.getEmail(), request.getPassword(),
                        request.getDisplayName(), tenantId)
                .map(Response::success);
    }

    @PostMapping("/email/login")
    public Mono<Response<TokenResponse>> emailLogin(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid EmailLoginRequest request,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "0.0.0.0") String ip) {
        return authService.emailLogin(request.getEmail(), request.getPassword(), tenantId, ip)
                .map(Response::success);
    }

    // ─── OAuth2 ───────────────────────────────────────────────────────────

    @GetMapping("/oauth2/{provider}/url")
    public Mono<Response<OAuthUrlResponse>> getOAuthUrl(
            @PathVariable String provider,
            @RequestParam(defaultValue = "http://localhost:8080/cc/api/v1/auth/oauth2/{provider}/callback") String redirectUri) {
        String state = UUID.randomUUID().toString();
        String key = "oauth:state:" + state;
        return redisTemplate.opsForValue().set(key, provider, Duration.ofMinutes(5))
                .then(Mono.defer(() -> {
                    for (OAuth2Service svc : oauth2Providers) {
                        if (svc.name().equalsIgnoreCase(provider)) {
                            String url = svc.getAuthorizationUrl(state, redirectUri);
                            return Mono.just(Response.success(new OAuthUrlResponse(url)));
                        }
                    }
                    return Mono.just(Response.error(400, "Unsupported OAuth2 provider: " + provider));
                }));
    }

    @PostMapping("/oauth2/{provider}/callback")
    public Mono<Response<TokenResponse>> oauth2Callback(
            @PathVariable String provider,
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid OAuthCallbackRequest request) {
        String stateKey = "oauth:state:" + request.getState();
        return redisTemplate.opsForValue().get(stateKey)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid or expired OAuth state")))
                .flatMap(storedProvider -> {
                    if (!storedProvider.equalsIgnoreCase(provider)) {
                        return Mono.error(new RuntimeException("OAuth provider mismatch"));
                    }
                    return redisTemplate.delete(stateKey);
                })
                .flatMap(ok -> {
                    for (OAuth2Service svc : oauth2Providers) {
                        if (svc.name().equalsIgnoreCase(provider)) {
                            return svc.exchangeCodeForUser(request.getCode(),
                                            "http://localhost:8080/cc/api/v1/auth/oauth2/" + provider + "/callback")
                                    .flatMap(userInfo -> authService.oauth2Login(
                                            userInfo.provider(), userInfo.subject(), userInfo.email(),
                                            userInfo.name(), userInfo.avatarUrl(), tenantId))
                                    .map(Response::success);
                        }
                    }
                    return Mono.just(Response.<TokenResponse>error(400, "Unsupported OAuth2 provider: " + provider));
                });
    }

    // ─── Email Verification ────────────────────────────────────────────────

    @GetMapping("/verify-email")
    public Mono<Response<Void>> verifyEmail(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam String token) {
        return authService.verifyEmail(token, tenantId)
                .thenReturn(Response.<Void>success());
    }

    // ─── Password Reset ────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public Mono<Response<Void>> forgotPassword(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid ForgotPasswordRequest request) {
        return authService.forgotPassword(request.getEmail(), tenantId)
                .thenReturn(Response.<Void>success());
    }

    @PostMapping("/reset-password")
    public Mono<Response<Void>> resetPassword(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid ResetPasswordRequest request) {
        return authService.resetPassword(request.getToken(), request.getNewPassword(), tenantId)
                .thenReturn(Response.<Void>success());
    }

    // ─── Token Management ─────────────────────────────────────────────────

    @PostMapping("/refresh")
    public Mono<Response<TokenResponse>> refresh(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestBody @Valid RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken(), tenantId)
                .map(Response::success);
    }

    @PostMapping("/logout")
    public Mono<Response<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Customer-ID") Long customerId) {
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        return authService.logout(token, customerId)
                .thenReturn(Response.<Void>success());
    }
}
