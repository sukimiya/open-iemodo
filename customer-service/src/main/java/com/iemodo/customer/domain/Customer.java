package com.iemodo.customer.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("customers")
public class Customer extends BaseEntity {

    private String tenantId;

    // ─── Login identifiers ─────────────────────────────────────────────────

    private String phone;
    private String email;
    private String oauthProvider;
    private String oauthSubject;

    /** BCrypt hashed password (only for email+password login). Null for OAuth/phone-only. */
    private String passwordHash;

    // ─── Profile ────────────────────────────────────────────────────────────

    private String displayName;
    private String firstName;
    private String lastName;
    @Column("avatar_url")
    private String avatarUrl;

    // ─── Verification ───────────────────────────────────────────────────────

    private Boolean phoneVerified;
    private Boolean emailVerified;

    // ─── Preferences ────────────────────────────────────────────────────────

    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;

    // ─── Last login ─────────────────────────────────────────────────────────

    private Instant lastLoginAt;
    private String lastLoginIp;

    // ─── Domain behaviour ──────────────────────────────────────────────────

    public boolean isOAuth() {
        return oauthProvider != null && !"LOCAL".equals(oauthProvider);
    }

    public boolean isActive() {
        return getStatus() != null && getStatus() == 1 && Boolean.TRUE.equals(getIsValid());
    }

    public Instant getCreatedAt() {
        return getCreateTime();
    }

    public Instant getUpdatedAt() {
        return getUpdateTime();
    }
}
