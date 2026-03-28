package com.iemodo.gateway.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Rate limit rule entity.
 * 
 * <p>Maps to the {@code rate_limit_rules} table in the {@code gateway_config} schema.
 * Defines rate limiting policies using Redis + Lua script.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("rate_limit_rules")
public class RateLimitRule extends BaseEntity {

    // id is inherited from BaseEntity

    /** Rule identifier (e.g., "default-public") */
    private String ruleName;

    /** Associated route ID (optional) */
    private String routeId;

    /** Key resolver strategy: PRINCIPAL_NAME | IP_ADDRESS | HEADER */
    private String keyResolver;

    /** Header name if key_resolver = HEADER */
    private String keyHeader;

    /** Tokens per second */
    private Integer replenishRate;

    /** Maximum bucket capacity */
    private Integer burstCapacity;

    /** Tokens consumed per request */
    private Integer requestedTokens;

    /** Custom Lua script (optional) */
    private String luaScript;

    /** Whether this rule is enabled */
    private Boolean enabled;

    /** Description of this rule */
    private String description;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public int getEffectiveReplenishRate() {
        return replenishRate != null ? replenishRate : 100;
    }

    public int getEffectiveBurstCapacity() {
        return burstCapacity != null ? burstCapacity : 200;
    }

    public int getEffectiveRequestedTokens() {
        return requestedTokens != null ? requestedTokens : 1;
    }

    /**
     * Generate a rate limiter key based on the key resolver strategy.
     */
    public String generateKey(String principal, String clientIp, String headerValue) {
        return switch (keyResolver) {
            case "PRINCIPAL_NAME" -> principal != null ? principal : "anonymous";
            case "IP_ADDRESS" -> clientIp != null ? clientIp : "unknown";
            case "HEADER" -> headerValue != null ? headerValue : "no-header";
            default -> "default";
        };
    }
}
