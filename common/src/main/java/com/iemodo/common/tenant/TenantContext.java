package com.iemodo.common.tenant;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Supplier;

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

    /**
     * Wrap a {@link Mono} producer so that it executes within the given tenant's
     * Reactor Context.  This is the main entry point for non-HTTP code paths
     * (scheduled tasks, message listeners, etc.) that need a tenant context.
     *
     * <p>Usage:
     * <pre>{@code
     * TenantContext.withTenant("tenant_001", () -> repository.findAll())
     *     .subscribe(...);
     * }</pre>
     *
     * @param tenantId the tenant to use for the operation
     * @param inner    supplier of the reactive pipeline
     * @param <T>      result type
     * @return a Mono that, when subscribed, will run {@code inner} inside the tenant context
     */
    public static <T> Mono<T> withTenant(String tenantId, Supplier<Mono<T>> inner) {
        return Mono.defer(inner)
                .contextWrite(ctx -> putTenantId(ctx, tenantId));
    }

    /**
     * Wrap a {@link reactor.core.publisher.Flux} producer with a tenant context.
     *
     * <p>Usage:
     * <pre>{@code
     * TenantContext.withTenant("tenant_001", () -> repository.findAll())
     *     .subscribe(...);
     * }</pre>
     *
     * @param tenantId the tenant to use for the operation
     * @param inner    supplier of the reactive pipeline
     * @param <T>      result type
     * @return a Flux that, when subscribed, will run {@code inner} inside the tenant context
     */
    public static <T> reactor.core.publisher.Flux<T> withTenantFlux(String tenantId, Supplier<reactor.core.publisher.Flux<T>> inner) {
        return reactor.core.publisher.Flux.defer(inner)
                .contextWrite(ctx -> putTenantId(ctx, tenantId));
    }

    /**
     * Put the tenant ID into the Reactor Context.
     */
    public static Context putTenantId(Context ctx, String tenantId) {
        return ctx.put(TENANT_ID_KEY, tenantId);
    }
}
