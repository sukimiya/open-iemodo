package com.iemodo.tenant.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Tracks a tenant's Stripe subscription.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("tenant_subscriptions")
public class TenantSubscription extends BaseEntity {

    private String tenantId;

    /** Stripe subscription ID (sub_xxx) */
    private String stripeSubscriptionId;

    /** Stripe customer ID (cus_xxx) */
    private String stripeCustomerId;

    /** Plan identifier: STANDARD / PROFESSIONAL / ENTERPRISE */
    private String planId;

    /** ACTIVE / PAST_DUE / CANCELED / INCOMPLETE */
    private String subscriptionStatus;

    /** Current period start */
    private Instant currentPeriodStart;

    /** Current period end */
    private Instant currentPeriodEnd;

    /** Cancel at period end (if user initiated cancellation) */
    private Boolean cancelAtPeriodEnd;

    /** Last Stripe invoice ID */
    private String lastInvoiceId;

    /** Number of billing cycles completed */
    private Integer billingCycleCount;

    // ─── Domain helpers ───────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(subscriptionStatus) && Boolean.TRUE.equals(getIsValid());
    }

    public boolean isPastDue() {
        return "PAST_DUE".equals(subscriptionStatus);
    }

    public boolean isCanceled() {
        return "CANCELED".equals(subscriptionStatus);
    }

    public void markActive(String subscriptionId, String customerId, String plan,
                           Instant periodStart, Instant periodEnd) {
        this.stripeSubscriptionId = subscriptionId;
        this.stripeCustomerId = customerId;
        this.planId = plan;
        this.subscriptionStatus = "ACTIVE";
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.cancelAtPeriodEnd = false;
    }

    public void markPastDue() {
        this.subscriptionStatus = "PAST_DUE";
    }

    public void markCanceled() {
        this.subscriptionStatus = "CANCELED";
    }

    public void markIncomplete() {
        this.subscriptionStatus = "INCOMPLETE";
    }
}
