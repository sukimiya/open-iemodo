package com.iemodo.rma.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iemodo.rma.domain.RmaRegionConfig;
import com.iemodo.rma.repository.RmaRegionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Resolves the effective {@link RmaRegionConfig} for a given tenant and region,
 * and serialises it to JSON for snapshotting.
 *
 * <p>Lookup precedence:
 * <ol>
 *   <li>Tenant-specific override (tenantId + regionCode)</li>
 *   <li>Platform default (null tenantId + regionCode)</li>
 *   <li>Built-in fallback (hardcoded sensible defaults)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RmaRegionConfigService {

    private final RmaRegionConfigRepository regionConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * Returns the effective config for {@code tenantId} + {@code regionCode},
     * falling back to platform defaults if no tenant-specific override exists.
     */
    public Mono<RmaRegionConfig> resolve(String tenantId, String regionCode) {
        return regionConfigRepository
                .findByRegionCodeAndTenantId(regionCode, tenantId)
                .switchIfEmpty(regionConfigRepository.findByRegionCodeAndTenantIdIsNull(regionCode))
                .switchIfEmpty(Mono.just(buildFallback(regionCode)))
                .doOnNext(cfg -> log.debug("Resolved region config: region={} tenant={} source={}",
                        regionCode, tenantId, cfg.getTenantId() == null ? "platform-default" : "tenant-override"));
    }

    /** Serialises a config to JSON for storing as a snapshot. */
    public String toSnapshot(RmaRegionConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise region config snapshot", e);
        }
    }

    /** Deserialises a previously stored snapshot. */
    public RmaRegionConfig fromSnapshot(String json) {
        try {
            return objectMapper.readValue(json, RmaRegionConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialise region config snapshot", e);
        }
    }

    // ─── Fallback defaults (when no DB config exists yet) ─────────────────

    private RmaRegionConfig buildFallback(String regionCode) {
        log.warn("No region config found for region={}, using built-in fallback", regionCode);
        return RmaRegionConfig.builder()
                .regionCode(regionCode)
                .returnWindowDays(30)
                .exchangeWindowDays(30)
                .shippingResponsibility("NEGOTIABLE")
                .taxRefundPolicy("IF_NOT_SHIPPED")
                .requireReason(true)
                .build();
    }
}
