package com.iemodo.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Geocode result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResult {

    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String countryCode;
    private String regionCode;
    private String city;
    private String postalCode;
    private String streetAddress;
    private String placeId;
}
