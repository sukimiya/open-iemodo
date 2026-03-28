package com.iemodo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request to create a new address.
 */
@Data
public class CreateAddressRequest {

    @Size(max = 100, message = "Address name must not exceed 100 characters")
    private String addressName;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    private String recipientName;

    @NotBlank(message = "Recipient phone is required")
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String recipientPhone;

    @Size(max = 200, message = "Email must not exceed 200 characters")
    private String recipientEmail;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
    private String countryCode;

    @Size(max = 10, message = "Region code must not exceed 10 characters")
    private String regionCode;

    @Size(max = 100, message = "Region name must not exceed 100 characters")
    private String regionName;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 500, message = "Address line 1 must not exceed 500 characters")
    private String addressLine1;

    @Size(max = 500, message = "Address line 2 must not exceed 500 characters")
    private String addressLine2;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    private Boolean isDefault = false;
    private Boolean isDefaultBilling = false;
}
