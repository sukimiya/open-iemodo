package com.iemodo.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Delivery estimate result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEstimateResult {

    private Long warehouseId;
    private String destinationCountry;
    private Integer estimatedDays;
    private Integer warehouseProcessingDays;
    private Integer customsClearanceDays;
    private Integer transitDays;
}
