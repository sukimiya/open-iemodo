package com.iemodo.tax.domain;

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
 * Tax exemption entity for B2B customers
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("tax_exemptions")
public class TaxExemption extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("customer_id")
    private Long customerId;

    @Column("tenant_id")
    private String tenantId;

    @Column("country_code")
    private String countryCode;

    @Column("tax_id_number")
    private String taxIdNumber;

    @Column("tax_id_type")
    private String taxIdType;

    @Column("company_name")
    private String companyName;

    @Column("is_verified")
    private Boolean isVerified;

    @Column("verification_source")
    private String verificationSource;

    @Column("verification_response")
    private String verificationResponse;

    @Column("valid_from")
    private LocalDate validFrom;

    @Column("valid_to")
    private LocalDate validTo;

    @Column("exemption_status")
    private ExemptionStatus exemptionStatus;

    public enum ExemptionStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        EXPIRED,
        REVOKED
    }

    /**
     * Check if exemption is currently valid
     */
    public boolean isValid() {
        if (exemptionStatus != ExemptionStatus.ACTIVE) return false;
        if (!Boolean.TRUE.equals(isVerified)) return false;
        
        LocalDate now = LocalDate.now();
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validTo != null && now.isAfter(validTo)) return false;
        
        return true;
    }
}
