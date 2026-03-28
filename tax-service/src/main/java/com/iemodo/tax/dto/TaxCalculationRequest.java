package com.iemodo.tax.dto;

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
 * Tax calculation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationRequest {

    @NotBlank(message = "Country code is required")
    private String countryCode;

    private String regionCode;
    private String postalCode;
    private String city;

    @NotEmpty(message = "Items are required")
    @Valid
    private List<TaxItem> items;

    private String customerTaxId;
    private Long customerId;
    private Boolean isB2b;

    private String currency;

    /**
     * Tax item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxItem {
        @NotBlank(message = "SKU is required")
        private String sku;

        @NotNull(message = "Amount is required")
        private BigDecimal amount;

        private String taxCategory;
        private Integer quantity;
    }
}
