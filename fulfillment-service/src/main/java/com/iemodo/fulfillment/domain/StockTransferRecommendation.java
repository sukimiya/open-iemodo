package com.iemodo.fulfillment.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock transfer recommendation entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("stock_transfer_recommendations")
public class StockTransferRecommendation extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("recommendation_no")
    private String recommendationNo;

    @Column("from_warehouse_id")
    private Long fromWarehouseId;

    @Column("to_warehouse_id")
    private Long toWarehouseId;

    @Column("analysis_type")
    private AnalysisType analysisType;

    @Column("lookback_days")
    private Integer lookbackDays;

    @Column("sku_id")
    private Long skuId;

    @Column("sku")
    private String sku;

    @Column("recommended_quantity")
    private Integer recommendedQuantity;

    @Column("reason")
    private String reason;

    @Column("priority")
    private Priority priority;

    @Column("projected_cost_savings")
    private BigDecimal projectedCostSavings;

    @Column("projected_delivery_improvement")
    private BigDecimal projectedDeliveryImprovement;

    @Column("recommendation_status")
    private Integer recommendationStatus;

    @Column("approved_by")
    private Long approvedBy;

    @Column("approved_at")
    private Instant approvedAt;

    @Column("executed_at")
    private Instant executedAt;

    @Column("tenant_id")
    private String tenantId;



    public enum AnalysisType {
        COUNTRY,
        REGION,
        SKU
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum RecommendationStatus {
        PENDING(0),
        APPROVED(1),
        REJECTED(2),
        EXECUTED(3);
        
        private final int value;
        
        RecommendationStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * Approve recommendation
     */
    public void approve(Long approverId) {
        this.recommendationStatus = RecommendationStatus.APPROVED.getValue();
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
    }

    /**
     * Mark as executed
     */
    public void markAsExecuted() {
        this.recommendationStatus = RecommendationStatus.EXECUTED.getValue();
        this.executedAt = Instant.now();
    }
}
