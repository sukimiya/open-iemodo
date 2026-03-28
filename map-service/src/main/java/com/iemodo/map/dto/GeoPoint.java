package com.iemodo.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Geographic point
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {

    private Double latitude;
    private Double longitude;

    public static GeoPoint of(Double lat, Double lng) {
        return new GeoPoint(lat, lng);
    }
}
