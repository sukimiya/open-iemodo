package com.iemodo.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * User address book entry — maps to the {@code user_addresses} table.
 * 
 * <p>Supports multiple addresses per user with default billing/shipping flags.
 * Uses GeoHash for location-based queries and address standardization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_addresses")
public class UserAddress {

    @Id
    private Long id;

    /** Reference to the owning user (users.id) */
    private Long customerId;

    // ─── Address Metadata ──────────────────────────────────────────────────

    /** Address alias (e.g., "Home", "Office", "Warehouse") */
    private String addressName;

    // ─── Recipient Information ─────────────────────────────────────────────

    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;

    // ─── Address Components ────────────────────────────────────────────────

    private String countryCode;
    private String regionCode;      // State/Province code
    private String regionName;      // State/Province name
    private String city;
    private String district;        // District/County
    private String addressLine1;    // Street address
    private String addressLine2;    // Apartment, suite, floor, etc.
    private String postalCode;

    // ─── Location & Verification ───────────────────────────────────────────

    /** GeoHash encoding for location-based queries */
    private String geoHash;

    /** Whether the address has been verified (e.g., via address validation API) */
    private Boolean isVerified;

    // ─── Default Flags ─────────────────────────────────────────────────────

    private Boolean isDefault;          // Default shipping address
    private Boolean isDefaultBilling;   // Default billing address

    // ─── Timestamps ────────────────────────────────────────────────────────

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain behaviour ─────────────────────────────────────────────────

    /**
     * Returns a formatted single-line address suitable for display.
     */
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

    /**
     * Checks if this address can be used for shipping.
     * Requires essential fields to be present.
     */
    public boolean isValidForShipping() {
        return recipientName != null && !recipientName.isBlank()
                && recipientPhone != null && !recipientPhone.isBlank()
                && addressLine1 != null && !addressLine1.isBlank()
                && city != null && !city.isBlank()
                && countryCode != null && !countryCode.isBlank();
    }
}
