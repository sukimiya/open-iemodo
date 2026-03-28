package com.iemodo.common.cache;

import com.iemodo.common.tenant.TenantContext;
import reactor.core.publisher.Mono;

/**
 * Utility class for building tenant-isolated cache keys.
 * 
 * <p>Ensures that cached data from one tenant is never accessible to another tenant
 * by prefixing all cache keys with the tenant identifier.
 * 
 * <p>Usage:
 * <pre>
 * String key = CacheKeyBuilder.forTenant("product:123");
 * // Result: "acme-corp:product:123"
 * </pre>
 */
public class CacheKeyBuilder {

    private static final String SEPARATOR = ":";

    private CacheKeyBuilder() {
        // Utility class
    }

    /**
     * Build a cache key with the current tenant context.
     * Must be called within a reactive pipeline where TenantContext is available.
     *
     * @param key the base key
     * @return the tenant-prefixed key
     */
    public static Mono<String> forCurrentTenant(String key) {
        return TenantContext.getTenantId()
                .map(tenantId -> buildKey(tenantId, key))
                .switchIfEmpty(Mono.just(key));  // Fallback if no tenant context
    }

    /**
     * Build a cache key with an explicit tenant ID.
     *
     * @param tenantId the tenant identifier
     * @param key      the base key
     * @return the tenant-prefixed key
     */
    public static String forTenant(String tenantId, String key) {
        return buildKey(tenantId, key);
    }

    /**
     * Build a cache key for global (cross-tenant) data.
     * Use sparingly - prefer tenant-isolated keys.
     *
     * @param key the base key
     * @return the global key with "global" prefix
     */
    public static String forGlobal(String key) {
        return "global" + SEPARATOR + key;
    }

    /**
     * Build a cache key for user-specific data within a tenant.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @param key      the base key
     * @return the tenant:user-prefixed key
     */
    public static String forUser(String tenantId, Long userId, String key) {
        return tenantId + SEPARATOR + "user:" + userId + SEPARATOR + key;
    }

    /**
     * Extract the base key from a tenant-prefixed key.
     *
     * @param prefixedKey the full cache key
     * @return the base key without tenant prefix
     */
    public static String extractBaseKey(String prefixedKey) {
        if (prefixedKey == null || prefixedKey.isEmpty()) {
            return prefixedKey;
        }
        int firstColon = prefixedKey.indexOf(SEPARATOR);
        if (firstColon == -1) {
            return prefixedKey;
        }
        return prefixedKey.substring(firstColon + 1);
    }

    /**
     * Extract the tenant ID from a tenant-prefixed key.
     *
     * @param prefixedKey the full cache key
     * @return the tenant ID, or null if not found
     */
    public static String extractTenantId(String prefixedKey) {
        if (prefixedKey == null || prefixedKey.isEmpty()) {
            return null;
        }
        int firstColon = prefixedKey.indexOf(SEPARATOR);
        if (firstColon == -1) {
            return null;
        }
        return prefixedKey.substring(0, firstColon);
    }

    private static String buildKey(String tenantId, String key) {
        if (tenantId == null || tenantId.isEmpty()) {
            return key;
        }
        return tenantId + SEPARATOR + key;
    }
}
