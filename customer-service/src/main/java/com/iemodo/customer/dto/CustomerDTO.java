package com.iemodo.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CustomerDTO {

    private Long id;
    private String tenantId;
    private String phone;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String oauthProvider;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;
    private Instant createdAt;
    private Instant updatedAt;
}
