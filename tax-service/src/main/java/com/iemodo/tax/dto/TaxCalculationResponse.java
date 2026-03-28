package com.iemodo.tax.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tax calculation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationResponse {

    private String countryCode;
    private String taxType;
    private String currency;

    private BigDecimal subtotal;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;

    private Boolean isExempt;
    private String exemptionReason;

    private List<TaxItemDetail> items;

    /**
     * Tax item detail
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxItemDetail {
        private String sku;
        private BigDecimal amount;
        private BigDecimal taxableAmount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private String taxCategory;
    }
}
