package com.iemodo.payment.service;

import com.iemodo.payment.domain.Payment;
import com.iemodo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled jobs for payment consistency:
 *
 * <ol>
 *   <li><b>Expired payment cancellation</b> (every 60 s) — cancels PENDING payments
 *       whose {@code expired_at} has passed.</li>
 *   <li><b>Stripe reconciliation</b> (every 5 min) — finds payments stuck in
 *       PENDING/PROCESSING for more than 10 minutes and queries Stripe for their
 *       current status, correcting any divergence caused by lost webhooks.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentService     paymentService;
    private final PaymentRepository  paymentRepository;
    private final StripePaymentProvider stripeProvider;

    // ─── Expired payment cancellation ────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    public void cancelExpiredPayments() {
        paymentService.processExpiredPayments()
                .subscribe(
                        count -> { if (count > 0) log.info("Cancelled {} expired payments", count); },
                        ex -> log.error("Error cancelling expired payments", ex)
                );
    }

    // ─── Stripe reconciliation (lost-webhook compensation) ───────────────────

    /**
     * Finds payments stuck in PENDING/PROCESSING for more than 10 minutes,
     * queries Stripe for their real status, and updates local state to match.
     *
     * <p>This is the compensation mechanism for the "lost webhook" scenario:
     * Stripe already settled the payment but our webhook never arrived.
     */
    @Scheduled(fixedDelay = 300_000)
    public void reconcileStuckPayments() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);

        paymentRepository.findStuckPayments(cutoff)
                .flatMap(payment ->
                        stripeProvider.retrievePayment(payment.getThirdPartyTxnId())
                                .flatMap(result -> reconcilePayment(payment, result))
                                .onErrorResume(ex -> {
                                    log.error("Failed to reconcile payment={}", payment.getId(), ex);
                                    return Mono.empty();
                                }))
                .subscribe(
                        null,
                        ex -> log.error("Reconciliation scheduler error", ex)
                );
    }

    /**
     * Applies the Stripe-side status to the local payment record if they diverge.
     */
    private Mono<Void> reconcilePayment(Payment payment, PaymentProvider.PaymentResult result) {
        if (!result.success()) return Mono.empty();

        return switch (result.status()) {
            case "succeeded" -> {
                if (!payment.isFinalState()) {
                    log.info("Reconcile: payment={} marked SUCCESS (Stripe: succeeded)", payment.getId());
                    payment.markAsPaid(result.transactionId());
                    yield paymentRepository.save(payment).then();
                }
                yield Mono.empty();
            }
            case "canceled" -> {
                if (!payment.isFinalState()) {
                    log.info("Reconcile: payment={} marked CANCELLED (Stripe: canceled)", payment.getId());
                    payment.setPaymentStatus(Payment.PaymentStatus.CANCELLED);
                    yield paymentRepository.save(payment).then();
                }
                yield Mono.empty();
            }
            case "requires_payment_method" -> {
                if (payment.isPending()) {
                    log.info("Reconcile: payment={} marked FAILED (Stripe: requires_payment_method)", payment.getId());
                    payment.markAsFailed("requires_payment_method", "Payment method declined");
                    yield paymentRepository.save(payment).then();
                }
                yield Mono.empty();
            }
            default -> Mono.empty();
        };
    }
}
