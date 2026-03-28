package com.iemodo.product.domain;

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
 * Brand entity - represents a product brand/manufacturer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("brands")
public class Brand {

    @Id
    private Long id;

    private String name;
    
    /** Localized name as JSON: {"en": "Apple", "zh": "苹果"} */
    private String nameLocalized;
    
    private String logoUrl;
    private String website;
    private String description;
    
    /** Brand origin country code (ISO 3166-1 alpha-2) */
    private String countryCode;
    
    private Integer sortOrder;
    private Boolean isActive;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return isActive != null && isActive;
    }
}
