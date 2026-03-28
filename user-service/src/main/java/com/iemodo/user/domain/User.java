package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * User aggregate root — maps to the {@code users} table
 * in the current tenant's Schema (routed by
 * {@link com.iemodo.common.tenant.PostgresTenantConnectionFactory}).
 * 
 * <p>Platform-level table containing tenant_id for cross-schema routing during login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;

    /** Platform-level tenant identifier for cross-schema routing */
    private String tenantId;

    private String email;

    /** BCrypt hashed password. Null for OAuth2-only users. */
    private String passwordHash;

    // ─── Basic Profile ─────────────────────────────────────────────────────

    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;

    // ─── OAuth2 ────────────────────────────────────────────────────────────

    /** Authentication provider: LOCAL, GOOGLE, FACEBOOK, APPLE */
    private String oauthProvider;

    /** Provider's unique subject identifier (e.g. Google sub). */
    private String oauthSubject;

    // ─── Status & Verification ─────────────────────────────────────────────

    /** ACTIVE | DISABLED | DELETED */
    private String status;

    private Boolean emailVerified;
    private Boolean phoneVerified;

    // ─── Preferences ───────────────────────────────────────────────────────

    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;

    // ─── Statistics ────────────────────────────────────────────────────────

    private Integer totalOrders;
    private BigDecimal totalSpent;

    // ─── Timestamps ────────────────────────────────────────────────────────

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;

    // ─── Domain behaviour ─────────────────────────────────────────────────

    public boolean isLocal() {
        return "LOCAL".equals(oauthProvider);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void softDelete() {
        this.status = "DELETED";
        this.deletedAt = Instant.now();
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return displayName;
    }
}
