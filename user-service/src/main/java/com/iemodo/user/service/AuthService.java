package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.RefreshToken;
import com.iemodo.user.domain.User;
import com.iemodo.user.dto.LoginRequest;
import com.iemodo.user.dto.RegisterRequest;
import com.iemodo.user.dto.TokenResponse;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.repository.RefreshTokenRepository;
import com.iemodo.user.repository.UserRepository;
import com.iemodo.user.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Core authentication service.
 *
 * <p>All operations are non-blocking — returns {@link Mono}.
 * The {@code tenantId} parameter identifies the schema routing context
 * and is embedded into the JWT payload as the {@code tid} claim.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService           jwtService;
    private final PasswordEncoder      passwordEncoder;

    // ─── Register ──────────────────────────────────────────────────────────

    @Transactional
    public Mono<TokenResponse> register(RegisterRequest request, String tenantId) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT));
                    }
                    User user = User.builder()
                            .tenantId(tenantId)
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .displayName(request.getDisplayName() != null
                                    ? request.getDisplayName()
                                    : request.getEmail().split("@")[0])
                            .oauthProvider("LOCAL")
                            .status(1)  // ACTIVE
                            .build();
                    return userRepository.save(user);
                })
                .flatMap(user -> buildTokenResponse(user, tenantId))
                .doOnSuccess(r -> log.info("Registered user email={} tenant={}", request.getEmail(), tenantId))
                .doOnError(e -> log.error("Registration failed email={} tenant={}: {}",
                        request.getEmail(), tenantId, e.getMessage()));
    }

    // ─── Login ─────────────────────────────────────────────────────────────

    public Mono<TokenResponse> login(LoginRequest request, String tenantId) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> {
                    if (!user.isActive()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Account is disabled"));
                    }
                    if (!user.isLocal() || user.getPasswordHash() == null) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                                "Account uses social login — use OAuth2"));
                    }
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));
                    }
                    return buildTokenResponse(user, tenantId);
                })
                .doOnSuccess(r -> log.info("Login success email={} tenant={}", request.getEmail(), tenantId))
                .doOnError(e -> log.warn("Login failed email={} tenant={}: {}",
                        request.getEmail(), tenantId, e.getMessage()));
    }

    // ─── Logout ────────────────────────────────────────────────────────────

    public Mono<Void> logout(String accessToken, Long userId) {
        return jwtService.blacklistToken(accessToken)
                .then(refreshTokenRepository.revokeAllByUserId(userId))
                .then()
                .doOnSuccess(v -> log.info("Logout successful userId={}", userId));
    }

    // ─── Refresh ───────────────────────────────────────────────────────────

    public Mono<TokenResponse> refresh(String rawRefreshToken, String tenantId) {
        String tokenHash = HashUtil.sha256(rawRefreshToken);

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)))
                .flatMap(rt -> {
                    if (!rt.isValid()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
                    }
                    return userRepository.findById(rt.getUserId())
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)));
                })
                .flatMap(user -> {
                    // Revoke old refresh token (rotation)
                    return refreshTokenRepository.revokeByTokenHash(tokenHash)
                            .then(buildTokenResponse(user, tenantId));
                });
    }

    // ─── OAuth2 ─────────────────────────────────────────────────────────────

    public Mono<TokenResponse> loginOrRegisterOAuth2(
            String provider, String subject, String email,
            String displayName, String avatarUrl, String tenantId) {

        return userRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .switchIfEmpty(
                        userRepository.findByEmail(email)
                                .flatMap(existing -> {
                                    // Link OAuth2 to existing email account
                                    existing.setOauthProvider(provider);
                                    existing.setOauthSubject(subject);
                                    if (existing.getAvatarUrl() == null) {
                                        existing.setAvatarUrl(avatarUrl);
                                    }
                                    return userRepository.save(existing);
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    // New OAuth2 user
                                    User newUser = User.builder()
                                            .tenantId(tenantId)
                                            .email(email)
                                            .oauthProvider(provider)
                                            .oauthSubject(subject)
                                            .displayName(displayName)
                                            .avatarUrl(avatarUrl)
                                            .status(1)  // ACTIVE
                                            .build();
                                    return userRepository.save(newUser);
                                }))
                )
                .flatMap(user -> buildTokenResponse(user, tenantId));
    }

    // ─── OAuth2 Callback ──────────────────────────────────────────────────

    /**
     * Handle OAuth2 authorization code callback.
     *
     * <p>In a full implementation, this would exchange {@code code} with the
     * provider's token endpoint (via WebClient) to obtain user info.
     * For now it delegates to the existing {@link #loginOrRegisterOAuth2} method
     * after extracting the identity from Spring Security's OAuth2 context.
     *
     * <p>The actual token exchange is managed by Spring Security OAuth2 Client;
     * this method is the post-authentication hook that issues an iemodo JWT pair.
     *
     * @param provider  OAuth2 provider name (e.g. "google")
     * @param code      authorization code from provider (placeholder — Spring Security handles exchange)
     * @param tenantId  tenant context from X-TenantID header
     */
    public Mono<TokenResponse> handleOAuth2Callback(
            String provider, String code, String tenantId) {
        // In production: use WebClient to exchange `code` for an id_token,
        // decode the id_token claims, then call loginOrRegisterOAuth2.
        // This stub returns an informative error so the API surface is correct.
        return Mono.error(new BusinessException(
                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "OAuth2 callback must be processed by Spring Security filter chain at /oauth2/authorization/" + provider
        ));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private Mono<TokenResponse> buildTokenResponse(User user, String tenantId) {
        String accessToken  = jwtService.generateAccessToken(user, tenantId);
        String rawRefresh   = jwtService.generateRawRefreshToken();
        String refreshHash  = HashUtil.sha256(rawRefresh);

        RefreshToken rt = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .expiresAt(Instant.now().plus(jwtService.refreshTokenTtlDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(rt)
                .thenReturn(TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(rawRefresh)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.accessTokenTtlSeconds())
                        .user(toUserDTO(user))
                        .build());
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .oauthProvider(user.getOauthProvider())
                .status(user.getStatus() != null && user.getStatus() == 1 ? "ACTIVE" : "DISABLED")
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .preferredCurrency(user.getPreferredCurrency())
                .preferredLanguage(user.getPreferredLanguage())
                .preferredCountry(user.getPreferredCountry())
                .totalOrders(user.getTotalOrders())
                .totalSpent(user.getTotalSpent())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
