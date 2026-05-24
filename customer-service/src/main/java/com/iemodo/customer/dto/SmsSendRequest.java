package com.iemodo.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsSendRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s-()]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;
}
