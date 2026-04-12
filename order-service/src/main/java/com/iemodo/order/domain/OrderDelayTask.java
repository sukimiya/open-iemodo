package com.iemodo.order.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("order_delay_task")
public class OrderDelayTask extends BaseEntity {

    private Long orderId;

    private String tenantId;

    /** Task type identifier, e.g. {@code PAYMENT_TIMEOUT}. */
    private String taskType;

    /** When this task should be executed. */
    @Column("execute_time")
    private Instant executeTime;

    @Column("task_status")
    private DelayTaskStatus taskStatus;
}
