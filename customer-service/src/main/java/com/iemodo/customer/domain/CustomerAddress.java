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
@Table("customer_addresses")
public class CustomerAddress extends BaseEntity {

    private Long customerId;

    private String addressName;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;

    private String countryCode;
    private String regionCode;
    private String regionName;
    private String city;
    private String district;
    private String addressLine1;
    private String addressLine2;
    private String postalCode;

    private String geoHash;
    private Boolean isVerified;

    private Boolean isDefault;
    private Boolean isDefaultBilling;

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null) sb.append(addressLine1);
        if (addressLine2 != null) sb.append(", ").append(addressLine2);
        if (city != null) sb.append(", ").append(city);
        if (regionName != null) sb.append(", ").append(regionName);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (countryCode != null) sb.append(", ").append(countryCode);
        return sb.toString();
    }

    public boolean isValidForShipping() {
        return recipientName != null && !recipientName.isBlank()
                && recipientPhone != null && !recipientPhone.isBlank()
                && addressLine1 != null && !addressLine1.isBlank()
                && city != null && !city.isBlank()
                && countryCode != null && !countryCode.isBlank();
    }

    public Instant getCreatedAt() {
        return getCreateTime();
    }

    public Instant getUpdatedAt() {
        return getUpdateTime();
    }
}
