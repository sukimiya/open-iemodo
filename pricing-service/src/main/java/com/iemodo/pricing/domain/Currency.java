package com.iemodo.pricing.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Currency entity - ISO 4217 currency codes
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("currencies")
public class Currency extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("code")
    private String code;  // ISO 4217: USD, EUR, CNY

    @Column("name")
    private String name;

    @Column("symbol")
    private String symbol;  // $, €, ¥

    @Column("decimal_places")
    private Integer decimalPlaces;

    @Column("is_active")
    private Boolean isActive;

    @Column("is_base_currency")
    private Boolean isBaseCurrency;  // USD is base
}
