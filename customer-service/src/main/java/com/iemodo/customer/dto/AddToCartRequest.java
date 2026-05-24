package com.iemodo.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotBlank(message = "SKU code is required")
    private String skuCode;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
