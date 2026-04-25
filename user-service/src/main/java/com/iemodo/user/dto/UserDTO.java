package com.iemodo.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * User data transfer object for API responses.
 */
@Data
@Builder
public class UserDTO {

    private Long id;
    private String tenantId;
    private String email;
    
    // Profile
    private String displayName;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String avatarUrl;
    
    // OAuth
    private String oauthProvider;
    
    // Status
    private String status;
    private String role;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    
    // Preferences
    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;
    
    // Statistics
    private Integer totalOrders;
    private BigDecimal totalSpent;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
