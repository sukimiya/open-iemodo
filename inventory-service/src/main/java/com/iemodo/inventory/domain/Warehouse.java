package com.iemodo.inventory.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Warehouse entity - represents a storage facility.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("warehouses")
public class Warehouse extends BaseEntity {
    // id is inherited from BaseEntity

    private String warehouseCode;
    private String name;
    private String nameLocalized;

    // Address
    private String countryCode;
    private String regionCode;
    private String city;
    private String district;
    private String address;
    private String postalCode;

    // Geolocation
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Type: STANDARD, EXPRESS, REFRIGERATED, BONDED
    private String warehouseType;

    // Service
    private String serviceLevel;
    private Integer avgProcessHours;

    // Status
    private Boolean isActive;
    private Boolean isDefault;

    // Capacity
    private Integer maxDailyOrders;
    private Integer currentDailyOrders;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public boolean isDefault() {
        return isDefault != null && isDefault;
    }

    public boolean hasCapacity() {
        if (maxDailyOrders == null) return true;
        int current = currentDailyOrders != null ? currentDailyOrders : 0;
        return current < maxDailyOrders;
    }

    /**
     * Calculate distance to another location using Haversine formula.
     * @return Distance in kilometers
     */
    public double distanceTo(BigDecimal targetLat, BigDecimal targetLon) {
        if (latitude == null || longitude == null || targetLat == null || targetLon == null) {
            return Double.MAX_VALUE;
        }

        final int R = 6371; // Earth's radius in km

        double lat1 = Math.toRadians(latitude.doubleValue());
        double lat2 = Math.toRadians(targetLat.doubleValue());
        double deltaLat = Math.toRadians(targetLat.doubleValue() - latitude.doubleValue());
        double deltaLon = Math.toRadians(targetLon.doubleValue() - longitude.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public enum WarehouseType {
        STANDARD, EXPRESS, REFRIGERATED, BONDED
    }

    public enum ServiceLevel {
        STANDARD, PREMIUM
    }
}
