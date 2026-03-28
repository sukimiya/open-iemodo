package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * OAuth2 provider connection — maps to the {@code oauth_connections} table.
 * 
 * <p>Tracks the linkage between a local user account and their third-party
 * OAuth2 identities (Google, Facebook, Apple, etc.).
 * 
 * <p>One user can have multiple OAuth connections (e.g., linked both Google
 * and Facebook), but each provider+subject combination is unique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("oauth_connections")
public class OAuthConnection {

    @Id
    private Long id;

    /** Reference to the local user (users.id) */
    private Long userId;

    /** OAuth provider: GOOGLE, FACEBOOK, APPLE */
    private String provider;

    /** Provider's unique subject identifier (e.g., Google 'sub' claim) */
    private String providerSubject;

    /** Email address from the provider (may differ from local email) */
    private String providerEmail;

    /** Access token for provider API calls (encrypted at rest) */
    private String accessToken;

    /** Refresh token for obtaining new access tokens */
    private String refreshToken;

    /** When the access token expires */
    private Instant tokenExpiresAt;

    @CreatedDate
    private Instant createdAt;

    // ─── Domain behaviour ─────────────────────────────────────────────────

    /**
     * Check if the access token is expired or about to expire (within 5 minutes).
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return true;
        }
        return Instant.now().plusSeconds(300).isAfter(tokenExpiresAt);
    }

    /**
     * Check if this connection is for a specific provider.
     */
    public boolean isProvider(String providerName) {
        return provider != null && provider.equalsIgnoreCase(providerName);
    }
}
