package com.iemodo.customer.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.customer.domain.Customer;
import com.iemodo.customer.domain.CustomerRefreshToken;
import com.iemodo.customer.dto.CustomerDTO;
import com.iemodo.customer.dto.TokenResponse;
import com.iemodo.customer.repository.CustomerRefreshTokenRepository;
import com.iemodo.customer.repository.CustomerRepository;
import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationType;
import com.iemodo.notification.dto.SendNotificationRequest;
import com.iemodo.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private static final Duration VERIFY_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final String VERIFY_PREFIX = "verify:email:";
    private static final String RESET_PREFIX = "reset:password:";

    private final CustomerRepository customerRepository;
    private final CustomerRefreshTokenRepository refreshTokenRepository;
    private final CustomerJwtService jwtService;
    private final SmsOtpService smsOtpService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${iemodo.storefront.base-url:http://localhost:3000}")
    private String storefrontBaseUrl;

    // ─── Phone OTP Login (auto-register if new) ───────────────────────────

    @Transactional
    public Mono<TokenResponse> smsLogin(String phone, String otpCode, String tenantId, String ipAddress) {
        return smsOtpService.verifyOtp(phone, otpCode, tenantId)
                .flatMap(ok -> customerRepository.findByPhoneAndTenantIdAndIsValid(phone, tenantId, true)
                        .switchIfEmpty(Mono.defer(() -> {
                            Customer newCustomer = Customer.builder()
                                    .tenantId(tenantId)
                                    .phone(phone)
                                    .phoneVerified(true)
                                    .displayName(phone)
                                    .preferredCurrency("USD")
                                    .preferredLanguage("en")
                                    .status(1)
                                    .build();
                            return customerRepository.save(newCustomer);
                        })))
                .flatMap(customer -> {
                    customer.setLastLoginAt(Instant.now());
                    customer.setLastLoginIp(ipAddress);
                    return customerRepository.save(customer);
                })
                .flatMap(customer -> buildTokenResponse(customer, tenantId))
                .doOnSuccess(r -> log.info("SMS login success phone={} tenant={}", phone, tenantId))
                .doOnError(e -> log.warn("SMS login failed phone={} tenant={}: {}", phone, tenantId, e.getMessage()));
    }

    // ─── Email + Password Register ────────────────────────────────────────

    @Transactional
    public Mono<TokenResponse> emailRegister(String email, String password, String displayName, String tenantId) {
        return customerRepository.existsByEmailAndTenantIdAndIsValid(email, tenantId, true)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.CUSTOMER_ALREADY_EXISTS, HttpStatus.CONFLICT));
                    }
                    Customer customer = Customer.builder()
                            .tenantId(tenantId)
                            .email(email)
                            .passwordHash(passwordEncoder.encode(password))
                            .displayName(displayName != null ? displayName : email.split("@")[0])
                            .emailVerified(false)
                            .status(1)
                            .build();
                    return customerRepository.save(customer);
                })
                .flatMap(customer -> {
                    String token = UUID.randomUUID().toString().replace("-", "");
                    String verifyKey = VERIFY_PREFIX + tenantId + ":" + token;
                    String verifyLink = storefrontBaseUrl + "/verify-email?token=" + token;

                    SendNotificationRequest notifyReq = new SendNotificationRequest();
                    notifyReq.setUserId(customer.getId());
                    notifyReq.setTenantId(tenantId);
                    notifyReq.setChannel(NotificationChannel.EMAIL);
                    notifyReq.setType(NotificationType.USER_REGISTERED);
                    notifyReq.setRecipient(email);
                    notifyReq.setLanguage(customer.getPreferredLanguage() != null
                            ? customer.getPreferredLanguage() : "en");
                    notifyReq.setVariables(Map.of(
                            "userName", customer.getDisplayName() != null
                                    ? customer.getDisplayName() : email,
                            "verifyLink", verifyLink
                    ));

                    return redisTemplate.opsForValue()
                            .set(verifyKey, String.valueOf(customer.getId()), VERIFY_TOKEN_TTL)
                            .then(notificationService.send(notifyReq)
                                    .onErrorResume(ex -> {
                                        log.warn("Welcome notification failed for {}: {}", email, ex.getMessage());
                                        return Mono.empty();
                                    }))
                            .then(buildTokenResponse(customer, tenantId));
                })
                .doOnSuccess(r -> log.info("Email register success email={} tenant={}", email, tenantId))
                .doOnError(e -> log.warn("Email register failed email={} tenant={}: {}", email, tenantId, e.getMessage()));
    }

    // ─── Email + Password Login ──────────────────────────────────────────

    public Mono<TokenResponse> emailLogin(String email, String password, String tenantId, String ipAddress) {
        return customerRepository.findByEmailAndTenantIdAndIsValid(email, tenantId, true)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.UNAUTHORIZED)))
                .flatMap(customer -> {
                    if (!customer.isActive()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Account is disabled"));
                    }
                    if (customer.getPasswordHash() == null) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                                "This account uses OAuth/phone login"));
                    }
                    if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));
                    }
                    customer.setLastLoginAt(Instant.now());
                    customer.setLastLoginIp(ipAddress);
                    return customerRepository.save(customer);
                })
                .flatMap(customer -> buildTokenResponse(customer, tenantId))
                .doOnSuccess(r -> log.info("Email login success email={} tenant={}", email, tenantId))
                .doOnError(e -> log.warn("Email login failed email={} tenant={}: {}", email, tenantId, e.getMessage()));
    }

    // ─── OAuth2 Login ─────────────────────────────────────────────────────

    @Transactional
    public Mono<TokenResponse> oauth2Login(
            String provider, String subject, String email,
            String displayName, String avatarUrl, String tenantId) {

        return customerRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .switchIfEmpty(Mono.defer(() -> {
                    if (email != null) {
                        return customerRepository.findByEmailAndTenantIdAndIsValid(email, tenantId, true)
                                .flatMap(existing -> {
                                    existing.setOauthProvider(provider);
                                    existing.setOauthSubject(subject);
                                    if (existing.getAvatarUrl() == null) {
                                        existing.setAvatarUrl(avatarUrl);
                                    }
                                    return customerRepository.save(existing);
                                });
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Customer newCustomer = Customer.builder()
                            .tenantId(tenantId)
                            .email(email)
                            .oauthProvider(provider)
                            .oauthSubject(subject)
                            .displayName(displayName)
                            .avatarUrl(avatarUrl)
                            .emailVerified(email != null)
                            .status(1)
                            .build();
                    return customerRepository.save(newCustomer);
                })))
                .flatMap(customer -> {
                    customer.setLastLoginAt(Instant.now());
                    return customerRepository.save(customer);
                })
                .flatMap(customer -> buildTokenResponse(customer, tenantId))
                .doOnSuccess(r -> log.info("OAuth2 login success provider={} sub={} tenant={}", provider, subject, tenantId))
                .doOnError(e -> log.warn("OAuth2 login failed provider={} sub={}: {}", provider, subject, e.getMessage()));
    }

    // ─── Logout ─────────────────────────────────────────────────────────

    public Mono<Void> logout(String accessToken, Long customerId) {
        return jwtService.blacklistToken(accessToken)
                .then(refreshTokenRepository.revokeAllByCustomerId(customerId))
                .then()
                .doOnSuccess(v -> log.info("Customer logout successful customerId={}", customerId));
    }

    // ─── Email Verification ───────────────────────────────────────────────

    public Mono<Void> verifyEmail(String token, String tenantId) {
        String verifyKey = VERIFY_PREFIX + tenantId + ":" + token;
        return redisTemplate.opsForValue().get(verifyKey)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TOKEN_INVALID, HttpStatus.BAD_REQUEST, "Invalid or expired verification token")))
                .flatMap(customerIdStr -> {
                    Long customerId = Long.valueOf(customerIdStr);
                    return redisTemplate.delete(verifyKey)
                            .then(customerRepository.findById(customerId))
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                            .flatMap(customer -> {
                                customer.setEmailVerified(true);
                                return customerRepository.save(customer);
                            });
                })
                .then()
                .doOnSuccess(v -> log.info("Email verified for token tenant={}", tenantId));
    }

    // ─── Forgot Password ──────────────────────────────────────────────────

    public Mono<Void> forgotPassword(String email, String tenantId) {
        return customerRepository.findByEmailAndTenantIdAndIsValid(email, tenantId, true)
                .flatMap(customer -> {
                    String token = UUID.randomUUID().toString().replace("-", "");
                    String resetKey = RESET_PREFIX + tenantId + ":" + token;
                    String resetLink = storefrontBaseUrl + "/reset-password?token=" + token;

                    SendNotificationRequest notifyReq = new SendNotificationRequest();
                    notifyReq.setUserId(customer.getId());
                    notifyReq.setTenantId(tenantId);
                    notifyReq.setChannel(NotificationChannel.EMAIL);
                    notifyReq.setType(NotificationType.PASSWORD_RESET);
                    notifyReq.setRecipient(email);
                    notifyReq.setLanguage(customer.getPreferredLanguage() != null
                            ? customer.getPreferredLanguage() : "en");
                    notifyReq.setVariables(Map.of(
                            "userName", customer.getDisplayName() != null
                                    ? customer.getDisplayName() : email,
                            "resetLink", resetLink
                    ));

                    return redisTemplate.opsForValue()
                            .set(resetKey, String.valueOf(customer.getId()), RESET_TOKEN_TTL)
                            .then(notificationService.send(notifyReq)
                                    .onErrorResume(ex -> {
                                        log.warn("Password reset email failed for {}: {}", email, ex.getMessage());
                                        return Mono.empty();
                                    }))
                            .then();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Password reset requested for unknown email: {}", email);
                    return Mono.<Void>empty();
                }))
                .doOnSuccess(v -> log.info("Password reset email sent to {}", email));
    }

    // ─── Reset Password ───────────────────────────────────────────────────

    @Transactional
    public Mono<Void> resetPassword(String token, String newPassword, String tenantId) {
        String resetKey = RESET_PREFIX + tenantId + ":" + token;
        return redisTemplate.opsForValue().get(resetKey)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TOKEN_INVALID, HttpStatus.BAD_REQUEST, "Invalid or expired reset token")))
                .flatMap(customerIdStr -> {
                    Long customerId = Long.valueOf(customerIdStr);
                    return redisTemplate.delete(resetKey)
                            .then(customerRepository.findById(customerId))
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                            .flatMap(customer -> {
                                customer.setPasswordHash(passwordEncoder.encode(newPassword));
                                return customerRepository.save(customer);
                            });
                })
                .then()
                .doOnSuccess(v -> log.info("Password reset completed for token tenant={}", tenantId));
    }

    // ─── Refresh ─────────────────────────────────────────────────────────

    public Mono<TokenResponse> refresh(String rawRefreshToken, String tenantId) {
        String tokenHash = sha256(rawRefreshToken);

        return refreshTokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED)))
                .flatMap(rt -> {
                    if (!rt.isValid()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
                    }
                    return customerRepository.findById(rt.getCustomerId())
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND)));
                })
                .flatMap(customer -> {
                    // Rotate: revoke old, issue new pair
                    return refreshTokenRepository.revokeByTokenHash(tokenHash)
                            .then(buildTokenResponse(customer, tenantId));
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Mono<TokenResponse> buildTokenResponse(Customer customer, String tenantId) {
        String accessToken = jwtService.generateAccessToken(customer, tenantId);
        String rawRefresh  = jwtService.generateRawRefreshToken();
        String refreshHash = sha256(rawRefresh);

        CustomerRefreshToken rt = CustomerRefreshToken.builder()
                .customerId(customer.getId())
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
                        .customer(toCustomerDTO(customer))
                        .build());
    }

    private CustomerDTO toCustomerDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .displayName(customer.getDisplayName())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .avatarUrl(customer.getAvatarUrl())
                .oauthProvider(customer.getOauthProvider())
                .phoneVerified(customer.getPhoneVerified())
                .emailVerified(customer.getEmailVerified())
                .preferredCurrency(customer.getPreferredCurrency())
                .preferredLanguage(customer.getPreferredLanguage())
                .preferredCountry(customer.getPreferredCountry())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private static String sha256(String input) {
        return com.iemodo.customer.service.SmsOtpService.sha256(input);
    }
}
