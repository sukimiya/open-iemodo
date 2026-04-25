package com.iemodo.user.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.user.domain.ConsentRecord;
import com.iemodo.user.domain.User;
import com.iemodo.user.dto.UserDTO;
import com.iemodo.user.repository.ConsentRecordRepository;
import com.iemodo.user.repository.OAuthConnectionRepository;
import com.iemodo.user.repository.RefreshTokenRepository;
import com.iemodo.user.repository.UserAddressRepository;
import com.iemodo.user.repository.UserDeviceRepository;
import com.iemodo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * GDPR compliance service: erasure, data portability, consent management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GDPRService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthConnectionRepository oauthConnectionRepository;
    private final UserDeviceRepository deviceRepository;
    private final ConsentRecordRepository consentRecordRepository;

    // ─── Right to Erasure (Art. 17) ─────────────────────────────────

    /**
     * Anonymize all personal data for a user. The account remains as a stub
     * for referential integrity but all PII is irreversibly replaced.
     */
    @Transactional
    public Mono<Void> eraseUserData(Long userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    user.setEmail("redacted-" + userId + "@anonymized");
                    user.setPasswordHash(null);
                    user.setDisplayName("[Deleted]");
                    user.setFirstName(null);
                    user.setLastName(null);
                    user.setPhone(null);
                    user.setAvatarUrl(null);
                    user.setOauthSubject(null);
                    user.setPreferredCurrency(null);
                    user.setPreferredLanguage(null);
                    user.setPreferredCountry(null);
                    user.setTotalOrders(0);
                    user.setTotalSpent(null);
                    user.setEmailVerified(false);
                    user.setPhoneVerified(false);
                    user.softDelete();

                    return userRepository.save(user);
                })
                // Cascade: delete personal data in related tables
                .flatMap(saved -> addressRepository.deleteAllByCustomerId(userId)
                        .then(refreshTokenRepository.revokeAllByUserId(userId))
                        .then(oauthConnectionRepository.deleteAllByUserId(userId))
                        .then(deviceRepository.revokeAllByUserId(userId, null))
                        .then(consentRecordRepository.findAllByUserId(userId)
                                .flatMap(consent -> {
                                    consent.setIsValid(false);
                                    return consentRecordRepository.save(consent);
                                })
                                .then()))
                .doOnSuccess(v -> log.info("GDPR erasure completed for userId={}", userId));
    }

    // ─── Data Portability (Art. 20) ─────────────────────────────────

    /**
     * Export all user data in a machine-readable format.
     */
    public Mono<Map<String, Object>> exportUserData(Long userId) {
        Map<String, Object> export = new HashMap<>();
        export.put("exportDate", Instant.now().toString());
        export.put("userId", userId);

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("email", user.getEmail());
                    profile.put("displayName", user.getDisplayName());
                    profile.put("firstName", user.getFirstName());
                    profile.put("lastName", user.getLastName());
                    profile.put("phone", user.getPhone());
                    profile.put("preferredCurrency", user.getPreferredCurrency());
                    profile.put("preferredLanguage", user.getPreferredLanguage());
                    profile.put("preferredCountry", user.getPreferredCountry());
                    profile.put("emailVerified", user.getEmailVerified());
                    profile.put("oauthProvider", user.getOauthProvider());
                    profile.put("createdAt", user.getCreatedAt());
                    profile.put("totalOrders", user.getTotalOrders());
                    profile.put("totalSpent", user.getTotalSpent());
                    export.put("profile", profile);

                    return addressRepository.findAllByCustomerId(userId).collectList();
                })
                .flatMap(addresses -> {
                    export.put("addresses", addresses);
                    return consentRecordRepository.findAllByUserId(userId).collectList();
                })
                .map(consents -> {
                    export.put("consents", consents);
                    return export;
                });
    }

    // ─── Consent Management (Art. 7) ────────────────────────────────

    /**
     * Get all consent records for a user.
     */
    public Flux<ConsentRecord> getConsents(Long userId) {
        return consentRecordRepository.findAllByUserId(userId);
    }

    /**
     * Set or update consent for a specific purpose.
     */
    @Transactional
    public Mono<ConsentRecord> setConsent(Long userId, String purpose, boolean given,
                                           String consentVersion, String ipAddress, String userAgent) {
        return consentRecordRepository.findByUserIdAndPurpose(userId, purpose)
                .flatMap(existing -> {
                    existing.setConsentGiven(given);
                    existing.setConsentDate(Instant.now());
                    existing.setConsentVersion(consentVersion != null ? consentVersion : existing.getConsentVersion());
                    existing.setIpAddress(ipAddress);
                    existing.setUserAgent(userAgent);
                    return consentRecordRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ConsentRecord record = ConsentRecord.builder()
                            .userId(userId)
                            .purpose(purpose)
                            .consentGiven(given)
                            .consentDate(Instant.now())
                            .consentVersion(consentVersion != null ? consentVersion : "1.0")
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .build();
                    return consentRecordRepository.save(record);
                }))
                .doOnSuccess(c -> log.info("Consent {} for userId={} purpose={}: {}",
                        c.getId() != null ? "updated" : "created", userId, purpose, given));
    }
}
