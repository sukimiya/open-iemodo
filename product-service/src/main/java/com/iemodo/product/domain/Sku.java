package com.iemodo.product.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * SKU (Stock Keeping Unit) entity - represents a specific product variant.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("skus")
public class Sku extends BaseEntity {
    // id is inherited from BaseEntity

    /** Parent product ID */
    private Long productId;

    /** Unique SKU code */
    private String skuCode;
    
    /** Barcode (EAN, UPC, etc.) */
    private String barcode;

    /**
     * Attributes as JSON: {"color": "red", "size": "XL", "material": "cotton"}
     */
    private String attributes;
    
    /** Hash of attributes for quick comparison */
    private String attributeHash;

    private String imageUrl;

    // Pricing
    private BigDecimal price;
    private BigDecimal costPrice;

    // Inventory
    private Integer stockQuantity;
    private Integer reservedQuantity;

    /**
     * Country availability.
     * Stored as comma-separated country codes in DB, parsed to array in Java.
     */
    private String availableInCountries;
    private String bannedInCountries;

    /** ACTIVE, OUT_OF_STOCK, DISABLED */
    private String skuStatus;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(skuStatus) && getIsValid() == 1;
    }

    public boolean isOutOfStock() {
        return "OUT_OF_STOCK".equals(skuStatus) || 
               (stockQuantity != null && stockQuantity <= 0);
    }

    public int getAvailableStock() {
        int stock = stockQuantity != null ? stockQuantity : 0;
        int reserved = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, stock - reserved);
    }

    public boolean hasEnoughStock(int quantity) {
        return getAvailableStock() >= quantity;
    }

    /**
     * Check if this SKU is available in a specific country.
     */
    public boolean isAvailableInCountry(String countryCode) {
        if (countryCode == null) return true;
        
        // Check whitelist
        if (availableInCountries != null && !availableInCountries.isEmpty()) {
            return availableInCountries.toUpperCase().contains(countryCode.toUpperCase());
        }
        
        // Check blacklist
        if (bannedInCountries != null && !bannedInCountries.isEmpty()) {
            return !bannedInCountries.toUpperCase().contains(countryCode.toUpperCase());
        }
        
        return true;
    }

    public void reserveStock(int quantity) {
        this.reservedQuantity = (reservedQuantity != null ? reservedQuantity : 0) + quantity;
    }

    public void releaseStock(int quantity) {
        this.reservedQuantity = Math.max(0, (reservedQuantity != null ? reservedQuantity : 0) - quantity);
    }

    public void deductStock(int quantity) {
        this.stockQuantity = Math.max(0, (stockQuantity != null ? stockQuantity : 0) - quantity);
        this.reservedQuantity = Math.max(0, (reservedQuantity != null ? reservedQuantity : 0) - quantity);
    }
}
