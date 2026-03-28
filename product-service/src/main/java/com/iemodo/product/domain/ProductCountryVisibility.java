package com.iemodo.product.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Product visibility and restrictions per country.
 * Controls which products are visible/purchasable in specific countries.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("product_country_visibility")
public class ProductCountryVisibility extends BaseEntity {
    // id is inherited from BaseEntity

    private Long productId;
    
    /** ISO 3166-1 alpha-2 country code */
    private String countryCode;
    
    /** Whether product is visible in catalog */
    private Boolean isVisible;
    
    /** Whether product can be purchased */
    private Boolean isPurchasable;
    
    /** Reason for restriction (e.g., "Missing FCC certification") */
    private String restrictionReason;
    
    /** Required certifications for this country */
    private String requiredCertifications;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isVisible() {
        return isVisible != null && isVisible;
    }

    public boolean isPurchasable() {
        return isPurchasable != null && isPurchasable;
    }

    /**
     * Create a default visibility record (visible and purchasable).
     */
    public static ProductCountryVisibility defaultVisibility(Long productId, String countryCode) {
        return ProductCountryVisibility.builder()
                .productId(productId)
                .countryCode(countryCode)
                .isVisible(true)
                .isPurchasable(true)
                .build();
    }

    /**
     * Create a restricted visibility record.
     */
    public static ProductCountryVisibility restricted(Long productId, String countryCode, String reason) {
        return ProductCountryVisibility.builder()
                .productId(productId)
                .countryCode(countryCode)
                .isVisible(false)
                .isPurchasable(false)
                .restrictionReason(reason)
                .build();
    }
}
