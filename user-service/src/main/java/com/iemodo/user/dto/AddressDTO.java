package com.iemodo.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Address data transfer object.
 */
@Data
@Builder
public class AddressDTO {

    private Long id;
    
    // Address Metadata
    private String addressName;
    
    // Recipient Information
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    
    // Address Components
    private String countryCode;
    private String regionCode;
    private String regionName;
    private String city;
    private String district;
    private String addressLine1;
    private String addressLine2;
    private String postalCode;
    
    // Location & Verification
    private String geoHash;
    private Boolean isVerified;
    
    // Default Flags
    private Boolean isDefault;
    private Boolean isDefaultBilling;
    
    // Formatted address for display
    private String formattedAddress;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
