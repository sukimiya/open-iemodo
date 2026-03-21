package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken {

    @Id
    private Long id;

    private Long userId;

    /** SHA-256 hash of the raw token sent to the client. */
    private String tokenHash;

    private String deviceId;
    private String userAgent;
    private String ipAddress;

    private Instant expiresAt;

    @CreatedDate
    private Instant createdAt;

    private boolean revoked;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
