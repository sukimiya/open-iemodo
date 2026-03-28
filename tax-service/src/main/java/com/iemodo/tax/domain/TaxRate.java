package com.iemodo.tax.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tax rate entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("tax_rates")
public class TaxRate extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("country_code")
    private String countryCode;

    @Column("region_code")
    private String regionCode;

    @Column("county_code")
    private String countyCode;

    @Column("city_code")
    private String cityCode;

    @Column("postal_code_start")
    private String postalCodeStart;

    @Column("postal_code_end")
    private String postalCodeEnd;

    @Column("postal_code_list")
    private String[] postalCodeList;

    @Column("tax_category")
    private String taxCategory;

    @Column("tax_type")
    private TaxType taxType;

    @Column("rate")
    private BigDecimal rate;

    @Column("is_compound")
    private Boolean isCompound;

    @Column("is_active")
    private Boolean isActive;

    @Column("effective_from")
    private LocalDate effectiveFrom;

    @Column("effective_to")
    private LocalDate effectiveTo;

    public enum TaxType {
        VAT,           // Value Added Tax (EU)
        GST,           // Goods and Services Tax (AU, NZ, SG, CA)
        SALES_TAX,     // Sales Tax (US)
        CONSUMPTION    // Consumption Tax (JP)
    }

    /**
     * Check if rate is currently effective
     */
    public boolean isEffective() {
        if (!Boolean.TRUE.equals(isActive)) return false;
        
        LocalDate now = LocalDate.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) return false;
        if (effectiveTo != null && now.isAfter(effectiveTo)) return false;
        
        return true;
    }

    /**
     * Calculate tax amount
     */
    public BigDecimal calculateTax(BigDecimal amount) {
        if (rate == null || amount == null) return BigDecimal.ZERO;
        return amount.multiply(rate);
    }
}
