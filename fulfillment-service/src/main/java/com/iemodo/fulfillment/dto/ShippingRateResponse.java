package com.iemodo.fulfillment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ShippingRateResponse {
    private String destinationCountry;
    private List<CarrierOption> carriers;
}

