package com.iemodo.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

    /**
     * Idempotency token obtained from {@code POST /orders/token}.
     * When provided, duplicate requests with the same token return the original order.
     */
    @Size(max = 64, message = "idempotencyKey must not exceed 64 characters")
    private String idempotencyKey;

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    // ─── Shipping ─────────────────────────────────────────────────────────
    private String shippingName;
    private String shippingPhone;
    private String shippingAddr;

    @Data
    public static class OrderItemRequest {

        @NotNull
        private Long productId;

        @NotBlank(message = "SKU is required")
        private String sku;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        @NotNull
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        private BigDecimal unitPrice;
    }
}
