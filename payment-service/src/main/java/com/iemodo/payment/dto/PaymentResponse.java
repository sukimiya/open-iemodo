package com.iemodo.payment.dto;

import com.iemodo.payment.domain.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Payment response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long customerId;

    private BigDecimal amount;
    private String currency;

    private String channel;
    private String channelSubType;

    private String status;
    
    // Payment method info (safe to expose)
    private String paymentMethodType;
    private String paymentMethodLast4;
    private String paymentMethodBrand;

    private String thirdPartyTxnId;

    private Instant paidAt;
    private Instant expiredAt;

    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;

    private String failureCode;
    private String failureMessage;

    private String description;
    private Map<String, Object> metadata;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert from entity to DTO
     */
    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) return null;
        
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId())
                .orderNo(payment.getOrderNo())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .channel(payment.getChannel() != null ? payment.getChannel().name() : null)
                .channelSubType(payment.getChannelSubType())
                .status(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null)
                .paymentMethodType(payment.getPaymentMethodType())
                .paymentMethodLast4(payment.getPaymentMethodLast4())
                .paymentMethodBrand(payment.getPaymentMethodBrand())
                .thirdPartyTxnId(payment.getThirdPartyTxnId())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .failureCode(payment.getFailureCode())
                .failureMessage(payment.getFailureMessage())
                .description(payment.getDescription())
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
