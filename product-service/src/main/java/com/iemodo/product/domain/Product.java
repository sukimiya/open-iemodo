package com.iemodo.product.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Product entity - main product information (SPU level).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("products")
public class Product {

    @Id
    private Long id;

    /** Unique product code */
    private String productCode;
    
    /** SPU (Standard Product Unit) code */
    private String spuCode;
    
    private String title;
    private String titleLocalized;
    private String description;
    private String descriptionLocalized;

    /** Category ID */
    private Long categoryId;
    
    /** Brand ID */
    private Long brandId;

    // Physical attributes
    private Integer weightG;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    // Pricing
    private BigDecimal basePrice;
    private BigDecimal costPrice;
    private BigDecimal marketPrice;

    // Status and flags
    /** DRAFT, ACTIVE, ARCHIVED */
    private String status;
    
    private Boolean isFeatured;
    private Boolean isNewArrival;

    // Customs and compliance
    private String hsCode;
    private String originCountry;
    private String certifications;

    // Media
    private String mainImage;
    private String videoUrl;

    // Attributes and search
    private String attributes;
    private String searchKeywords;
    private String searchVector;

    // Statistics
    private Integer viewCount;
    private Integer saleCount;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
    
    private Instant deletedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(status) && deletedAt == null;
    }

    public boolean isDraft() {
        return "DRAFT".equals(status);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(status) || deletedAt != null;
    }

    public boolean isFeatured() {
        return isFeatured != null && isFeatured;
    }

    public boolean isNewArrival() {
        return isNewArrival != null && isNewArrival;
    }

    public void softDelete() {
        this.status = "ARCHIVED";
        this.deletedAt = Instant.now();
    }

    public void incrementViewCount() {
        this.viewCount = (viewCount != null ? viewCount : 0) + 1;
    }

    public void incrementSaleCount(int quantity) {
        this.saleCount = (saleCount != null ? saleCount : 0) + quantity;
    }
}
