package com.iemodo.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Create refund request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefundRequest {

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Reason type is required")
    private String reasonType; // CUSTOMER_REQUEST, DUPLICATE, FRAUDULENT, etc.

    private String reasonDescription;

    private String orderId; // For validation
}
