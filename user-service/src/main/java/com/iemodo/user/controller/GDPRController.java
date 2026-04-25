package com.iemodo.user.controller;

import com.iemodo.common.response.Response;
import com.iemodo.user.domain.ConsentRecord;
import com.iemodo.user.service.GDPRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * GDPR compliance endpoints.
 *
 * <p>Base path: /uc/api/v1/gdpr
 */
@Slf4j
@RestController
@RequestMapping("/uc/api/v1/gdpr")
@RequiredArgsConstructor
public class GDPRController {

    private final GDPRService gdprService;

    /**
     * Request erasure of all personal data (Right to Erasure, Art. 17).
     */
    @PostMapping("/erasure")
    public Mono<Response<Void>> requestErasure(@RequestHeader("X-User-ID") Long userId) {
        return gdprService.eraseUserData(userId)
                .then(Mono.just(Response.success()));
    }

    /**
     * Export all user data in JSON format (Data Portability, Art. 20).
     */
    @GetMapping("/export")
    public Mono<Response<Map<String, Object>>> exportData(@RequestHeader("X-User-ID") Long userId) {
        return gdprService.exportUserData(userId)
                .map(Response::success);
    }

    // ─── Consent Management ─────────────────────────────────────────

    /**
     * Get all consent records for the current user.
     */
    @GetMapping("/consents")
    public Flux<Response<ConsentRecord>> getConsents(@RequestHeader("X-User-ID") Long userId) {
        return gdprService.getConsents(userId)
                .map(Response::success);
    }

    /**
     * Set or update consent for a specific purpose.
     */
    @PostMapping("/consents")
    public Mono<Response<ConsentRecord>> setConsent(
            @RequestHeader("X-User-ID") Long userId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestParam String purpose,
            @RequestParam boolean given,
            @RequestParam(defaultValue = "1.0") String version) {
        return gdprService.setConsent(userId, purpose, given, version, ipAddress, userAgent)
                .map(Response::success);
    }
}
