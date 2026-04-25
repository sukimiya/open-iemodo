package com.iemodo.tenant.domain;

import lombok.Getter;

import java.util.Map;

/**
 * Subscription plan definitions with feature limits.
 */
@Getter
public enum Plan {

    STANDARD(
        "STANDARD",
        "Starter",
        "stripe_price_standard",   // placeholder, replace with real Stripe Price ID
        1_000,     // max products
        500,       // max SKUs
        1_000,     // max orders / month
        100_000,   // API calls / day
        500,       // storage MB
        1,         // max admin users
        5          // max staff accounts
    ),
    PROFESSIONAL(
        "PROFESSIONAL",
        "Professional",
        "stripe_price_professional",
        10_000,    // max products
        5_000,     // max SKUs
        10_000,    // max orders / month
        1_000_000, // API calls / day
        5_000,     // storage MB
        5,         // max admin users
        50         // max staff accounts
    ),
    ENTERPRISE(
        "ENTERPRISE",
        "Enterprise",
        "stripe_price_enterprise",
        Integer.MAX_VALUE, // unlimited products
        Integer.MAX_VALUE, // unlimited SKUs
        Integer.MAX_VALUE, // unlimited orders
        Integer.MAX_VALUE, // unlimited API calls
        50_000,    // storage MB
        Integer.MAX_VALUE, // unlimited admins
        Integer.MAX_VALUE  // unlimited staff
    );

    private final String id;
    private final String displayName;
    private final String stripePriceId;
    private final int maxProducts;
    private final int maxSkus;
    private final int maxOrdersPerMonth;
    private final int maxApiCallsPerDay;
    private final int maxStorageMb;
    private final int maxAdminUsers;
    private final int maxStaffAccounts;

    Plan(String id, String displayName, String stripePriceId,
         int maxProducts, int maxSkus, int maxOrdersPerMonth,
         int maxApiCallsPerDay, int maxStorageMb,
         int maxAdminUsers, int maxStaffAccounts) {
        this.id = id;
        this.displayName = displayName;
        this.stripePriceId = stripePriceId;
        this.maxProducts = maxProducts;
        this.maxSkus = maxSkus;
        this.maxOrdersPerMonth = maxOrdersPerMonth;
        this.maxApiCallsPerDay = maxApiCallsPerDay;
        this.maxStorageMb = maxStorageMb;
        this.maxAdminUsers = maxAdminUsers;
        this.maxStaffAccounts = maxStaffAccounts;
    }

    public static Plan fromString(String planType) {
        if (planType == null) return STANDARD;
        for (Plan p : values()) {
            if (p.id.equalsIgnoreCase(planType)) return p;
        }
        return STANDARD;
    }

    /**
     * All limits as a map for API responses.
     */
    public Map<String, Object> getLimits() {
        return Map.of(
            "maxProducts", maxProducts,
            "maxSkus", maxSkus,
            "maxOrdersPerMonth", maxOrdersPerMonth,
            "maxApiCallsPerDay", maxApiCallsPerDay,
            "maxStorageMb", maxStorageMb,
            "maxAdminUsers", maxAdminUsers,
            "maxStaffAccounts", maxStaffAccounts
        );
    }
}
