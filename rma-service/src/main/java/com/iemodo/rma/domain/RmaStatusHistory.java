package com.iemodo.rma.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Immutable audit trail — one row per state transition on an {@link RmaRequest}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("rma_status_history")
public class RmaStatusHistory extends BaseEntity {

    private Long rmaId;

    private RmaStatus fromStatus;
    private RmaStatus toStatus;

    private Long operatorId;

    /**
     * Who triggered the transition.
     * CUSTOMER | MERCHANT | SYSTEM
     */
    private String operatorType;

    private String remark;
}
