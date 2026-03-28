package com.iemodo.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Route calculation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {

    private BigDecimal distance; // in kilometers
    private Integer durationMinutes;
    private String polyline;
}
