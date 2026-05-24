package com.iemodo.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAddressRequest {
    @Size(max = 100)
    private String addressName;

    @NotBlank
    @Size(max = 100)
    private String recipientName;

    @NotBlank
    @Size(max = 30)
    private String recipientPhone;

    @Size(max = 200)
    private String recipientEmail;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryCode;

    @Size(max = 10)
    private String regionCode;

    @Size(max = 100)
    private String regionName;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 500)
    private String addressLine1;

    @Size(max = 500)
    private String addressLine2;

    @Size(max = 20)
    private String postalCode;

    private Boolean isDefault = false;
    private Boolean isDefaultBilling = false;
}
