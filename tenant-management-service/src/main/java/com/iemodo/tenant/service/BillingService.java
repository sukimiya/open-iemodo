package com.iemodo.tenant.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.tenant.domain.Plan;
import com.iemodo.tenant.domain.TenantSubscription;
import com.iemodo.tenant.repository.TenantRepository;
import com.iemodo.tenant.repository.TenantSubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Billing service — Stripe Checkout, subscription lifecycle, webhooks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantProvisioningService provisioningService;

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${iemodo.billing.success-url:https://app.iemodo.com/settings/billing?success=true}")
    private String successUrl;

    @Value("${iemodo.billing.cancel-url:https://app.iemodo.com/settings/billing?canceled=true}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isEmpty()) {
            Stripe.apiKey = secretKey;
        }
    }

    /**
     * Create a Stripe Checkout Session for a tenant to subscribe or change plan.
     */
    public Mono<String> createCheckoutSession(String tenantId, String planId) {
        return tenantRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(tenant -> subscriptionRepository.findByTenantId(tenantId)
                        .defaultIfEmpty(new TenantSubscription()))
                .flatMap(sub -> Mono.fromCallable(() -> {
                    Plan plan = Plan.fromString(planId);

                    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .putExtraParam("idempotency_key", "cs_" + tenantId + "_" + planId + "_" + Instant.now().toEpochMilli())
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setPrice(plan.getStripePriceId())
                                            .setQuantity(1L)
                                            .build()
                            );

                    // If tenant already has a Stripe customer ID, attach to existing customer
                    if (sub.getStripeCustomerId() != null) {
                        paramsBuilder.setCustomer(sub.getStripeCustomerId());
                    }

                    // Metadata for webhook processing
                    paramsBuilder.putMetadata("tenant_id", tenantId);

                    Session session = Session.create(paramsBuilder.build());
                    return session.getUrl();
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnNext(url -> log.info("Checkout session created for tenant={} plan={}", tenantId, planId));
    }

    /**
     * Handle Stripe webhook events (checkout.session.completed, invoice.*, customer.subscription.*).
     */
    public Mono<String> handleWebhook(String payload, String signature) {
        return Mono.fromCallable(() -> {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            log.info("Stripe billing webhook: type={} id={}", event.getType(), event.getId());

            return switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event);
                case "invoice.paid" -> handleInvoicePaid(event);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
                default -> {
                    log.debug("Unhandled billing webhook type: {}", event.getType());
                    yield "ignored";
                }
            };
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String handleCheckoutCompleted(Event event) throws StripeException {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return "error: no session data";

        String tenantId = session.getMetadata().get("tenant_id");
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        // Retrieve full subscription for period details
        Subscription subscription = Subscription.retrieve(subscriptionId);
        String planId = subscription.getItems().getData().getFirst().getPrice().getMetadata().get("plan_id");
        if (planId == null) planId = "STANDARD";

        Instant periodStart = Instant.ofEpochSecond(subscription.getCurrentPeriodStart());
        Instant periodEnd = Instant.ofEpochSecond(subscription.getCurrentPeriodEnd());

        // Upsert subscription record
        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId)
                .defaultIfEmpty(new TenantSubscription())
                .block();

        if (sub == null || sub.getId() == null) {
            sub = TenantSubscription.builder()
                    .tenantId(tenantId)
                    .stripeCustomerId(customerId)
                    .build();
        }

        sub.markActive(subscriptionId, customerId, planId, periodStart, periodEnd);
        subscriptionRepository.save(sub).block();

        // Update tenant plan
        var tenant = tenantRepository.findByTenantId(tenantId).block();
        if (tenant != null) {
            tenant.setPlanType(planId);
            tenantRepository.save(tenant).block();
        }

        // Provision schemas if this is a new tenant checkout
        provisioningService.provisionSchemas(tenantId).block();

        log.info("Tenant {} subscribed to plan {} (sub={})", tenantId, planId, subscriptionId);
        return "ok";
    }

    private String handleInvoicePaid(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) return "error: no invoice data";

        String subscriptionId = invoice.getSubscription();
        var sub = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).block();
        if (sub == null) return "no subscription found";

        sub.setLastInvoiceId(invoice.getId());
        sub.setBillingCycleCount(sub.getBillingCycleCount() != null ? sub.getBillingCycleCount() + 1 : 1);

        if (invoice.getPeriodStart() != null) {
            sub.setCurrentPeriodStart(Instant.ofEpochSecond(invoice.getPeriodStart()));
        }
        if (invoice.getPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(Instant.ofEpochSecond(invoice.getPeriodEnd()));
        }

        subscriptionRepository.save(sub).block();
        return "ok";
    }

    private String handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) return "error: no invoice data";

        String subscriptionId = invoice.getSubscription();
        var sub = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).block();
        if (sub == null) return "no subscription found";

        sub.markPastDue();
        subscriptionRepository.save(sub).block();

        log.warn("Payment failed for tenant {} subscription {}", sub.getTenantId(), subscriptionId);
        return "ok";
    }

    private String handleSubscriptionUpdated(Event event) {
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return "error: no subscription data";

        var sub = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).block();
        if (sub == null) return "no subscription found";

        sub.setCancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd());
        sub.setCurrentPeriodStart(Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()));
        sub.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));

        String status = stripeSub.getStatus();
        if ("active".equals(status)) sub.markActive(
                sub.getStripeSubscriptionId(), sub.getStripeCustomerId(), sub.getPlanId(),
                sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd());
        else if ("past_due".equals(status)) sub.markPastDue();
        else if ("canceled".equals(status)) sub.markCanceled();

        subscriptionRepository.save(sub).block();
        return "ok";
    }

    private String handleSubscriptionDeleted(Event event) {
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return "error: no subscription data";

        var sub = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).block();
        if (sub == null) return "no subscription found";

        sub.markCanceled();
        subscriptionRepository.save(sub).block();

        log.warn("Subscription canceled for tenant {}", sub.getTenantId());
        return "ok";
    }

    // ─── Subscription management ─────────────────────────────────

    /**
     * Get the current subscription for a tenant.
     */
    public Mono<TenantSubscription> getSubscription(String tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    /**
     * Cancel subscription at period end.
     */
    @Transactional
    public Mono<Void> cancelAtPeriodEnd(String tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "No active subscription")))
                .flatMap(sub -> Mono.fromCallable(() -> {
                    Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
                    SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(true)
                            .build();
                    stripeSub.update(params);
                    sub.setCancelAtPeriodEnd(true);
                    return subscriptionRepository.save(sub);
                }).subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    /**
     * Get plan details with limits.
     */
    public Mono<Map<String, Object>> getPlanDetails(String planId) {
        Plan plan = Plan.fromString(planId);
        Map<String, Object> details = new java.util.HashMap<>(plan.getLimits());
        details.put("id", plan.getId());
        details.put("displayName", plan.getDisplayName());
        details.put("stripePriceId", plan.getStripePriceId());
        return Mono.just(details);
    }

    /**
     * Get details for all available plans.
     */
    public Mono<Map<String, Map<String, Object>>> getAllPlans() {
        Map<String, Map<String, Object>> all = new LinkedHashMap<>();
        for (Plan plan : Plan.values()) {
            Map<String, Object> details = new java.util.HashMap<>(plan.getLimits());
            details.put("id", plan.getId());
            details.put("displayName", plan.getDisplayName());
            details.put("stripePriceId", plan.getStripePriceId());
            all.put(plan.getId(), details);
        }
        return Mono.just(all);
    }
}
