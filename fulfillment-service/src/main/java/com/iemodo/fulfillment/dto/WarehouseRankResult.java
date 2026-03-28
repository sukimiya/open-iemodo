package com.iemodo.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Warehouse rank result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRankResult {

    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;
    
    private BigDecimal compositeScore;
    private BigDecimal costScore;
    private BigDecimal speedScore;
    private BigDecimal availabilityScore;
    private BigDecimal serviceLevelScore;
    
    private BigDecimal shippingCost;
    private Integer estimatedDeliveryDays;
    private BigDecimal distanceKm;
}
