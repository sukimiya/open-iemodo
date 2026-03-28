package com.iemodo.marketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Update coupon request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCouponRequest {

    private String name;
    private String description;
    private Boolean isActive;
    private Instant validTo;
}
