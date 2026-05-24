package com.iemodo.fulfillment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CarrierOption {
    private String carrierName;
    private String carrierCode;
    private String serviceType;
    private String serviceDescription;
    private BigDecimal shippingCost;
    private BigDecimal estimatedTax;
    private int estimatedDeliveryDays;
    private boolean isFastest;
    private boolean isCheapest;
}
