package com.iemodo.customer.dto;

import lombok.Data;

@Data
public class UpdateCustomerRequest {

    private String displayName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String preferredCurrency;
    private String preferredLanguage;
    private String preferredCountry;
}
