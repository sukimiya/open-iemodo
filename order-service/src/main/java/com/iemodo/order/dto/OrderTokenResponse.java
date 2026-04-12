package com.iemodo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderTokenResponse {

    /** Pre-generated order number to use as the idempotency key for order creation. */
    private String idempotencyKey;
}
