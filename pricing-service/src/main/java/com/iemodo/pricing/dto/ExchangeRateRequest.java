package com.iemodo.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Exchange rate request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateRequest {

    @NotBlank(message = "From currency is required")
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    private String toCurrency;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
