package com.iemodo.customer.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("customer_otp_records")
public class OtpRecord extends BaseEntity {

    private String tenantId;
    private String phone;

    /** SHA-256 hash of the generated OTP code. */
    private String otpHash;

    /** LOGIN | VERIFY_PHONE */
    private String purpose;

    private Boolean verified;
    private Instant expiresAt;
    private String ipAddress;
}
