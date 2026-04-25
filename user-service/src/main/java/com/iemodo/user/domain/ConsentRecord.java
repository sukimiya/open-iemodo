package com.iemodo.user.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Records user consent for data processing purposes (GDPR Art. 7).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("consent_records")
public class ConsentRecord extends BaseEntity {

    private Long userId;

    /** MARKETING | ANALYTICS | PERSONALIZATION | PROFILING | THIRD_PARTY */
    private String purpose;

    private Boolean consentGiven;

    private Instant consentDate;

    /** Version of the consent text the user agreed to */
    private String consentVersion;

    private String ipAddress;

    private String userAgent;
}
