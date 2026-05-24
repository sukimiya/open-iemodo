package com.iemodo.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AddressDTO {
    private Long id;
    private String addressName;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String countryCode;
    private String regionCode;
    private String regionName;
    private String city;
    private String district;
    private String addressLine1;
    private String addressLine2;
    private String postalCode;
    private String geoHash;
    private Boolean isVerified;
    private Boolean isDefault;
    private Boolean isDefaultBilling;
    private String formattedAddress;
    private Instant createdAt;
    private Instant updatedAt;
}
