package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * User aggregate root — maps to the {@code users} table
 * in the current tenant's Schema (routed by
 * {@link com.iemodo.common.tenant.PostgresTenantConnectionFactory}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;

    private String email;

    /** BCrypt hashed password. Null for OAuth2-only users. */
    private String passwordHash;

    private String displayName;
    private String avatarUrl;

    /** Authentication provider: LOCAL, GOOGLE, FACEBOOK */
    private String oauthProvider;

    /** Provider's unique subject identifier (e.g. Google sub). */
    private String oauthSubject;

    /** ACTIVE | DISABLED | DELETED */
    private String status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain behaviour ────────────────────────────────────────────────

    public boolean isLocal() {
        return "LOCAL".equals(oauthProvider);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void softDelete() {
        this.status = "DELETED";
    }
}
