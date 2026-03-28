package com.iemodo.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Warehouse with distance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDistance {

    private String warehouseId;
    private String warehouseName;
    private Double latitude;
    private Double longitude;
    private BigDecimal distanceKm;
}
