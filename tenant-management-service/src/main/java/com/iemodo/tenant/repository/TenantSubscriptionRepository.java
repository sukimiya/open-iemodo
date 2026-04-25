package com.iemodo.tenant.repository;

import com.iemodo.tenant.domain.TenantSubscription;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TenantSubscriptionRepository extends ReactiveCrudRepository<TenantSubscription, Long> {

    @Query("SELECT * FROM tenant_subscriptions WHERE tenant_id = :tenantId AND is_valid = TRUE LIMIT 1")
    Mono<TenantSubscription> findByTenantId(String tenantId);

    @Query("SELECT * FROM tenant_subscriptions WHERE stripe_customer_id = :stripeCustomerId AND is_valid = TRUE LIMIT 1")
    Mono<TenantSubscription> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT * FROM tenant_subscriptions WHERE stripe_subscription_id = :stripeSubscriptionId AND is_valid = TRUE LIMIT 1")
    Mono<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
