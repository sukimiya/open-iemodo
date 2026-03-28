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
 * Category entity - hierarchical product categorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("categories")
public class Category {

    @Id
    private Long id;

    /** Parent category ID for hierarchical structure */
    private Long parentId;

    private String name;
    
    /** Localized name as JSON */
    private String nameLocalized;
    
    private String description;
    private String descriptionLocalized;
    
    private String imageUrl;
    
    /** Category level: 1=root, 2=sub, 3=leaf */
    private Integer level;
    
    /** Path like /1/5/23 for easy traversal */
    private String path;
    
    private Integer sortOrder;
    private Boolean isActive;
    
    private String seoKeywords;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public boolean isRoot() {
        return level != null && level == 1;
    }

    public boolean isLeaf() {
        return level != null && level >= 3;
    }

    /**
     * Get the full path array from root to this category.
     */
    public Long[] getPathArray() {
        if (path == null || path.isEmpty()) {
            return new Long[0];
        }
        String[] parts = path.split("/");
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException e) {
                    // Ignore invalid parts
                }
            }
        }
        return ids.toArray(new Long[0]);
    }
}
