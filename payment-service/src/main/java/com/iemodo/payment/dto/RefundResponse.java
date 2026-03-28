package com.iemodo.payment.dto;

import com.iemodo.payment.domain.Refund;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Refund response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Long id;
    private String refundNo;
    private Long paymentId;
    private Long orderId;

    private BigDecimal amount;
    private String currency;

    private String status;
    private String reasonType;
    private String reasonDescription;

    private String thirdPartyRefundId;
    private Instant processedAt;

    private Map<String, Object> metadata;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert from entity to DTO
     */
    public static RefundResponse fromEntity(Refund refund) {
        if (refund == null) return null;
        
        return RefundResponse.builder()
                .id(refund.getId())
                .refundNo(refund.getRefundNo())
                .paymentId(refund.getPaymentId())
                .orderId(refund.getOrderId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .status(refund.getRefundStatus() != null ? refund.getRefundStatus().name() : null)
                .reasonType(refund.getReasonType() != null ? refund.getReasonType().name() : null)
                .reasonDescription(refund.getReasonDescription())
                .thirdPartyRefundId(refund.getThirdPartyRefundId())
                .processedAt(refund.getProcessedAt())
                .metadata(refund.getMetadata())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
