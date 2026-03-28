package com.iemodo.map.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Distance calculation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceRequest {

    @NotNull(message = "Origin is required")
    private GeoPoint origin;

    @NotNull(message = "Destination is required")
    private GeoPoint destination;
}
