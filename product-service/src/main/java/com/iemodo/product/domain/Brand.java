package com.iemodo.product.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Brand entity - represents a product brand/manufacturer.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("brands")
public class Brand extends BaseEntity {
    // id is inherited from BaseEntity

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

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return isActive != null && isActive;
    }
}
