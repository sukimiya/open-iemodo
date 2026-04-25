package com.iemodo.user.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * User aggregate root — maps to the {@code users} table
 * in the current tenant's Schema (routed by
 * {@link com.iemodo.common.tenant.PostgresTenantConnectionFactory}).
 * 
 * <p>Extends {@link BaseEntity} for automatic auditing fields:
 * id, status, createBy, createTime, updateBy, updateTime, isValid.
 * 
 * <p>Platform-level table containing tenant_id for cross-schema routing during login.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("users")
public class User extends BaseEntity {

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

    // ─── Verification ──────────────────────────────────────────────────────

    private Boolean emailVerified;
    private Boolean phoneVerified;

    // ─── Preferences ───────────────────────────────────────────────────────

    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;

    // ─── Statistics ────────────────────────────────────────────────────────

    private Integer totalOrders;
    private BigDecimal totalSpent;

    // ─── Legacy Soft Delete Timestamp (kept for backward compatibility) ─────

    private Instant deletedAt;

    // ─── RBAC ────────────────────────────────────────────────────────────────

    private String role;

    // ─── Domain behaviour ─────────────────────────────────────────────────

    public boolean isLocal() {
        return "LOCAL".equals(oauthProvider);
    }

    /**
     * Check if user is active (status=1 from BaseEntity and not deleted)
     */
    public boolean isActive() {
        return getStatus() != null && getStatus() == 1 && (deletedAt == null);
    }

    /**
     * Soft delete - marks user as invalid and sets deleted timestamp
     */
    public void softDelete() {
        setStatus(0); // Disabled
        setIsValid(false); // Invalid/Deleted
        this.deletedAt = Instant.now();
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return displayName;
    }

    // ─── Compatibility methods for existing code ───────────────────────────

    /**
     * 兼容方法：获取创建时间（返回 BaseEntity 的 createTime）
     */
    public Instant getCreatedAt() {
        return getCreateTime();
    }

    /**
     * 兼容方法：获取更新时间（返回 BaseEntity 的 updateTime）
     */
    public Instant getUpdatedAt() {
        return getUpdateTime();
    }
}
