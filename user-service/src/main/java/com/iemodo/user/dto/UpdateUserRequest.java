package com.iemodo.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request to update user profile.
 */
@Data
public class UpdateUserRequest {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters (ISO 4217)")
    private String preferredCurrency;

    @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
    private String preferredLanguage;

    @Size(min = 2, max = 2, message = "Country code must be 2 characters (ISO 3166-1 alpha-2)")
    private String preferredCountry;
}
