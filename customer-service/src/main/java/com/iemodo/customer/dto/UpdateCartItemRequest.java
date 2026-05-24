package com.iemodo.customer.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateCartItemRequest {
    @Min(value = 0, message = "Quantity must be 0 or more (0 removes the item)")
    private int quantity;
}
