package com.iemodo.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Warehouse rank request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRankRequest {

    private String destinationCountry;
    private String destinationPostalCode;
    private List<WarehouseAllocationRequest.AllocationItem> items;
}
