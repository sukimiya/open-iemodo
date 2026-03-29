package com.iemodo.rma.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RmaItemDTO {

    private Long id;
    private Long orderItemId;
    private Long productId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String reason;
    private String condition;
}
