package com.iemodo.gateway.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Gateway route configuration entity.
 * 
 * <p>Maps to the {@code gateway_routes} table in the {@code gateway_config} schema.
 * These routes are loaded dynamically and can be updated without restarting the gateway.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("gateway_routes")
public class GatewayRoute extends BaseEntity {

    // id is inherited from BaseEntity

    /** Route identifier (e.g., "user-service-route") */
    private String routeId;

    /** Target URI (e.g., "lb://user-service") */
    private String uri;

    /** Path predicate (e.g., "/uc/**") */
    private String path;

    /** HTTP method filter (optional) */
    private String method;

    /** Route priority (lower = higher priority) */
    private Integer priority;

    /** Whether this route is enabled */
    private Boolean enabled;

    /** Additional metadata as JSON */
    private String metadata;

    /** Description of this route */
    private String description;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public int getEffectivePriority() {
        return priority != null ? priority : 0;
    }
}
