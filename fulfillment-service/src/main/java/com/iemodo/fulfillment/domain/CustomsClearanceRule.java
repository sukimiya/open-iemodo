package com.iemodo.fulfillment.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/**
 * Customs clearance rule entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("customs_clearance_rules")
public class CustomsClearanceRule extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("origin_country")
    private String originCountry;

    @Column("destination_country")
    private String destinationCountry;

    @Column("clearance_hours")
    private Integer clearanceHours;

    @Column("is_same_country")
    private Boolean isSameCountry;

    @Column("is_customs_union")
    private Boolean isCustomsUnion;

    @Column("customs_union_code")
    private String customsUnionCode;

    @Column("description")
    private String description;

    @Column("is_active")
    private Boolean isActive;

    @Column("effective_from")
    private LocalDate effectiveFrom;

    @Column("effective_to")
    private LocalDate effectiveTo;
}
