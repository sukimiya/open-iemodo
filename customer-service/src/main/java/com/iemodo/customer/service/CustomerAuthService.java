package com.iemodo.customer.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.customer.domain.Customer;
import com.iemodo.customer.domain.CustomerRefreshToken;
import com.iemodo.customer.dto.CustomerDTO;
import com.iemodo.customer.dto.TokenResponse;
import com.iemodo.customer.repository.CustomerRefreshTokenRepository;
import com.iemodo.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final CustomerRefreshTokenRepository refreshTokenRepository;
    private final CustomerJwtService jwtService;
    private final SmsOtpService smsOtpService;
    private final PasswordEncoder passwordEncoder;

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
                            .status(1)
                            .build();
                    return customerRepository.save(customer);
                })
                .flatMap(customer -> buildTokenResponse(customer, tenantId))
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
