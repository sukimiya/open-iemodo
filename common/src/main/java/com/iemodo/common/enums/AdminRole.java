package com.iemodo.common.enums;

public enum AdminRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    SUPPORT,
    BILLING,
    ANALYST;

    public static AdminRole fromString(String role) {
        if (role == null) return TENANT_ADMIN;
        for (AdminRole r : values()) {
            if (r.name().equalsIgnoreCase(role)) return r;
        }
        return TENANT_ADMIN;
    }
}
