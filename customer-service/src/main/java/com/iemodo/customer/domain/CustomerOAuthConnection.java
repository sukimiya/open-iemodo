package com.iemodo.customer.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("customer_oauth_connections")
public class CustomerOAuthConnection extends BaseEntity {

    private Long customerId;

    private String provider;
    private String providerSubject;

    @Column("provider_email")
    private String providerEmail;

    private String accessToken;
    private String refreshToken;
    private String idToken;

    private Instant tokenExpiresAt;

    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) return true;
        return Instant.now().plusSeconds(300).isAfter(tokenExpiresAt);
    }
}
