package com.iemodo.map.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Geocode request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeRequest {

    @NotBlank(message = "Address is required")
    private String address;

    private String countryCode;
}
