package com.iemodo.common.tenant;

import reactor.core.publisher.Mono;

/**
 * Constants and utilities for tenant context used in Reactor Context.
 */
public final class TenantContext {

    /**
     * Key used to store the tenant ID in Reactor Context.
     * Set by {@link TenantIdWebFilter} and read by
     * {@link PostgresTenantConnectionFactory}.
     */
    public static final String TENANT_ID_KEY = "TENANT_ID";

    private TenantContext() {
        // utility class
    }

    /**
     * Get the current tenant ID from the Reactor Context.
     * Must be called within a reactive pipeline where the tenant ID has been set.
     *
     * @return Mono containing the tenant ID, or empty if not set
     */
    public static Mono<String> getTenantId() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(TENANT_ID_KEY)) {
                return Mono.just(ctx.get(TENANT_ID_KEY));
            }
            return Mono.empty();
        });
    }
}
