package com.iemodo.fulfillment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Warehouse allocation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseAllocationRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Destination country is required")
    private String destinationCountry;

    private String destinationRegion;
    private String destinationPostalCode;
    private String destinationCity;
    private BigDecimal destinationLat;
    private BigDecimal destinationLng;

    @NotEmpty(message = "Items are required")
    @Valid
    private List<AllocationItem> items;

    private AllocationPreference preference;

    public enum AllocationPreference {
        COST,      // Cost priority
        SPEED,     // Speed priority
        BALANCED   // Balanced (default)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationItem {
        @NotBlank(message = "SKU is required")
        private String sku;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}
