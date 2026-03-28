package com.iemodo.common.util;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {
        // Utility class
    }

    // ─── HTTP Headers ──────────────────────────────────────────────────────

    public static final String HEADER_TENANT_ID = "X-TenantID";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_TRACE_ID = "X-Trace-ID";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // ─── Security ──────────────────────────────────────────────────────────

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    // ─── OAuth2 Providers ──────────────────────────────────────────────────

    public static final String OAUTH_PROVIDER_LOCAL = "LOCAL";
    public static final String OAUTH_PROVIDER_GOOGLE = "GOOGLE";
    public static final String OAUTH_PROVIDER_FACEBOOK = "FACEBOOK";
    public static final String OAUTH_PROVIDER_APPLE = "APPLE";

    // ─── User Status ───────────────────────────────────────────────────────

    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_DISABLED = "DISABLED";
    public static final String USER_STATUS_DELETED = "DELETED";

    // ─── Tenant Status ─────────────────────────────────────────────────────

    public static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    public static final String TENANT_STATUS_SUSPENDED = "SUSPENDED";
    public static final String TENANT_STATUS_DELETED = "DELETED";

    // ─── Order Status ──────────────────────────────────────────────────────

    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_PAID = "PAID";
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_STATUS_REFUNDED = "REFUNDED";

    // ─── Payment Status ────────────────────────────────────────────────────

    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";

    // ─── Device Types ──────────────────────────────────────────────────────

    public static final String DEVICE_TYPE_MOBILE = "MOBILE";
    public static final String DEVICE_TYPE_WEB = "WEB";
    public static final String DEVICE_TYPE_TABLET = "TABLET";

    // ─── Cache Key Prefixes ────────────────────────────────────────────────

    public static final String CACHE_PREFIX_USER = "user:";
    public static final String CACHE_PREFIX_SESSION = "session:";
    public static final String CACHE_PREFIX_TOKEN_BLACKLIST = "token:blacklist:";
    public static final String CACHE_PREFIX_RATE_LIMIT = "rate_limit:";
    public static final String CACHE_PREFIX_INVENTORY = "inventory:";
    public static final String CACHE_PREFIX_PRODUCT = "product:";

    // ─── Time Constants (in seconds) ───────────────────────────────────────

    public static final long SECONDS_PER_MINUTE = 60;
    public static final long SECONDS_PER_HOUR = 3600;
    public static final long SECONDS_PER_DAY = 86400;

    // ─── Pagination Defaults ───────────────────────────────────────────────

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // ─── Regex Patterns ────────────────────────────────────────────────────

    public static final String PATTERN_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public static final String PATTERN_PHONE = "^[+]?[0-9\\s-()]+$";
    public static final String PATTERN_UUID = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    // ─── Currency Defaults ─────────────────────────────────────────────────

    public static final String DEFAULT_CURRENCY = "USD";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String DEFAULT_COUNTRY = "US";

    // ─── API Rate Limits ───────────────────────────────────────────────────

    public static final int RATE_LIMIT_PUBLIC = 100;      // requests per minute
    public static final int RATE_LIMIT_AUTH = 1000;       // requests per minute
    public static final int RATE_LIMIT_ADMIN = 5000;      // requests per minute
}
