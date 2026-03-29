package com.iemodo.rma.dto;

import com.iemodo.rma.domain.RmaStatus;
import com.iemodo.rma.domain.RmaType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RmaDTO {

    private Long id;
    private String rmaNo;
    private String tenantId;
    private Long orderId;
    private Long customerId;

    private RmaType type;
    private RmaStatus rmaStatus;

    private String regionCode;
    private String reason;
    private String description;

    private BigDecimal refundAmount;
    private String refundCurrency;
    private Boolean taxRefundIncluded;

    private String trackingNo;
    private String carrier;
    private String merchantNotes;

    private Instant approvedAt;
    private Instant receivedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private List<RmaItemDTO> items;
}
