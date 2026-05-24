package com.iemodo.customer.service;

import com.iemodo.customer.domain.OtpRecord;
import com.iemodo.customer.repository.OtpRecordRepository;
import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
public class SmsOtpService {

    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofSeconds(60);
    private static final String REDIS_KEY_PREFIX = "otp:";
    private static final String RATE_LIMIT_PREFIX = "otp:ratelimit:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final OtpRecordRepository otpRecordRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public SmsOtpService(ReactiveStringRedisTemplate redisTemplate,
                         OtpRecordRepository otpRecordRepository) {
        this.redisTemplate = redisTemplate;
        this.otpRecordRepository = otpRecordRepository;
    }

    public Mono<Void> generateAndSend(String phone, String tenantId, String ipAddress) {
        String rateLimitKey = RATE_LIMIT_PREFIX + tenantId + ":" + phone;

        return redisTemplate.opsForValue().setIfAbsent(rateLimitKey, "1", RATE_LIMIT_TTL)
                .flatMap(acquired -> {
                    if (Boolean.FALSE.equals(acquired)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.OTP_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS));
                    }

                    String otpCode = generateOtp();
                    String otpHash = sha256(otpCode);
                    String redisKey = REDIS_KEY_PREFIX + tenantId + ":" + phone;

                    // In production: send SMS via notification-service SmsChannelSender
                    log.info("=== SMS OTP for {}: {} ===", phone, otpCode);

                    return redisTemplate.opsForValue()
                            .set(redisKey, otpHash, OTP_TTL)
                            .then(otpRecordRepository.save(OtpRecord.builder()
                                    .tenantId(tenantId)
                                    .phone(phone)
                                    .otpHash(otpHash)
                                    .purpose("LOGIN")
                                    .verified(false)
                                    .expiresAt(Instant.now().plusSeconds(OTP_TTL.toSeconds()))
                                    .ipAddress(ipAddress)
                                    .build()))
                            .then();
                });
    }

    public Mono<Boolean> verifyOtp(String phone, String otpCode, String tenantId) {
        String redisKey = REDIS_KEY_PREFIX + tenantId + ":" + phone;

        return redisTemplate.opsForValue().get(redisKey)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.OTP_INVALID, HttpStatus.UNAUTHORIZED)))
                .flatMap(storedHash -> {
                    String inputHash = sha256(otpCode);
                    if (!storedHash.equals(inputHash)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.OTP_INVALID, HttpStatus.UNAUTHORIZED));
                    }
                    return redisTemplate.delete(redisKey)
                            .thenReturn(true);
                });
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
