package com.iemodo.order.dto;

import com.iemodo.order.domain.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderDTO {

    private Long id;
    private String orderNo;
    private String tenantId;
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddr;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemDTO> items;
}
