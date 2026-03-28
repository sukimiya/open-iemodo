package com.iemodo.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Gateway route configuration entity.
 * 
 * <p>Maps to the {@code gateway_routes} table in the {@code gateway_config} schema.
 * These routes are loaded dynamically and can be updated without restarting the gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_routes")
public class GatewayRoute {

    @Id
    private Long id;

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

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public int getEffectivePriority() {
        return priority != null ? priority : 0;
    }
}
