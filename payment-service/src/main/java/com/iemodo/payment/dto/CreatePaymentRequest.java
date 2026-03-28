package com.iemodo.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Create payment intent request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Order number is required")
    private String orderNo;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Payment channel is required")
    private String channel; // STRIPE, PAYPAL, etc.

    private String channelSubType; // CARD, IDEAL, etc.

    private String paymentMethodId; // For saved payment methods

    private String description;

    private Map<String, Object> metadata;

    private String returnUrl; // For redirect-based payments

    private String cancelUrl;
}
