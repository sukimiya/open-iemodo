package com.iemodo.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Stock transfer result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferResult {

    private Long transferId;
    private String recommendationNo;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private String sku;
    private Integer quantity;
    private String status;
    private Instant executedAt;
}
