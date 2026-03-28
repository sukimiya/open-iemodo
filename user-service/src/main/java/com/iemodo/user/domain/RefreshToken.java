package com.iemodo.user.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("refresh_tokens")
public class RefreshToken extends BaseEntity {
    // id is inherited from BaseEntity

    private Long userId;

    /** SHA-256 hash of the raw token sent to the client. */
    private String tokenHash;

    private String deviceId;
    private String userAgent;
    private String ipAddress;

    private Instant expiresAt;

    private boolean revoked;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
