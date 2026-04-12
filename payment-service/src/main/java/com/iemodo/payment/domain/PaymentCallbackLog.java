package com.iemodo.payment.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Idempotency log for incoming payment callbacks (webhooks).
 *
 * <p>Each Stripe event is recorded here before processing. The UNIQUE constraint
 * on {@code event_id} means a duplicate delivery causes a {@code DataIntegrityViolationException},
 * which the caller catches and swallows — guaranteeing exactly-once processing.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("payment_callback_log")
public class PaymentCallbackLog extends BaseEntity {

    private String eventId;
    private String eventType;
    private String paymentIntentId;
}
