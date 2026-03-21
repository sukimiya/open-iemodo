package com.iemodo.common.tenant;

/**
 * Constants for tenant context keys used in Reactor Context.
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
}
