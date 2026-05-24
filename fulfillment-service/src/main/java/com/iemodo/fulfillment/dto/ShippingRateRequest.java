package com.iemodo.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ShippingRateRequest {
    @NotBlank
    private String destinationCountry;

    private String destinationRegion;

    private String destinationPostalCode;

    private List<ShippingItem> items;

    @Data
    public static class ShippingItem {
        private String sku;
        private int quantity;
    }
}
