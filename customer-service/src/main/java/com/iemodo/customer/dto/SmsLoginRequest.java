package com.iemodo.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SmsLoginRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s-()]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;
}
