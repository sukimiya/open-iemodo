package com.iemodo.product.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Product entity - main product information (SPU level).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("products")
public class Product extends BaseEntity {
    // id is inherited from BaseEntity

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
    private String productStatus;
    
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

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(productStatus) && Boolean.TRUE.equals(getIsValid());
    }

    public boolean isDraft() {
        return "DRAFT".equals(productStatus);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(productStatus) || Boolean.FALSE.equals(getIsValid());
    }

    public boolean isFeatured() {
        return isFeatured != null && isFeatured;
    }

    public boolean isNewArrival() {
        return isNewArrival != null && isNewArrival;
    }

    public void softDelete() {
        this.productStatus = "ARCHIVED";
        setIsValid(false);
    }

    public void incrementViewCount() {
        this.viewCount = (viewCount != null ? viewCount : 0) + 1;
    }

    public void incrementSaleCount(int quantity) {
        this.saleCount = (saleCount != null ? saleCount : 0) + quantity;
    }
}
