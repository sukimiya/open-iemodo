package com.iemodo.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Warehouse allocation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseAllocationResult {

    private String orderId;
    private Long allocatedWarehouseId;
    private String warehouseName;
    private String warehouseCode;
    
    private BigDecimal shippingCost;
    private Integer estimatedDeliveryDays;
    private String shippingMethod;
    
    private List<AllocatedItem> items;
    
    private AllocationScore score;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllocatedItem {
        private String sku;
        private Integer quantity;
        private Boolean available;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllocationScore {
        private BigDecimal compositeScore;
        private BigDecimal costScore;
        private BigDecimal speedScore;
        private BigDecimal availabilityScore;
        private BigDecimal serviceLevelScore;
    }
}
