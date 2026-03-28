package com.iemodo.payment.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.payment.domain.Payment;
import com.iemodo.payment.domain.Refund;
import com.iemodo.payment.dto.*;
import com.iemodo.payment.repository.PaymentRepository;
import com.iemodo.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment service - Core payment business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final StripePaymentProvider stripeProvider;

    private static final Duration PAYMENT_EXPIRATION = Duration.ofMinutes(30);

    /**
     * Create payment intent
     */
    @Transactional
    public Mono<PaymentResponse> createPaymentIntent(CreatePaymentRequest request, String tenantId) {
        // Generate unique payment number
        String paymentNo = generatePaymentNo();
        
        // Set expiration time
        Instant expiredAt = Instant.now().plus(PAYMENT_EXPIRATION);

        Payment.PaymentChannel channel = Payment.PaymentChannel.valueOf(request.getChannel());

        // Create payment record
        Payment payment = Payment.builder()
                .paymentNo(paymentNo)
                .orderId(request.getOrderId())
                .orderNo(request.getOrderNo())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .channel(channel)
                .channelSubType(request.getChannelSubType())
                .paymentMethodId(request.getPaymentMethodId())
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .expiredAt(expiredAt)
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .tenantId(tenantId)
                .build();

        return paymentRepository.save(payment)
                .flatMap(savedPayment -> {
                    // Call Stripe to create payment intent
                    if (channel == Payment.PaymentChannel.STRIPE) {
                        return stripeProvider.createPaymentIntent(
                                request.getAmount(),
                                request.getCurrency(),
                                request.getPaymentMethodId(),
                                request.getMetadata(),
                                request.getReturnUrl()
                        ).flatMap(result -> {
                            if (result.success()) {
                                savedPayment.setThirdPartyTxnId(result.paymentIntentId());
                                savedPayment.setThirdPartyTxnData(result.rawResponse());
                                return paymentRepository.save(savedPayment)
                                        .map(PaymentResponse::fromEntity);
                            } else {
                                savedPayment.markAsFailed(result.errorCode(), result.errorMessage());
                                return paymentRepository.save(savedPayment)
                                        .flatMap(p -> Mono.error(new BusinessException(
                                                ErrorCode.BAD_REQUEST, 
                                                HttpStatus.BAD_REQUEST, 
                                                "Payment creation failed: " + result.errorMessage())));
                            }
                        });
                    }
                    return Mono.just(PaymentResponse.fromEntity(savedPayment));
                })
                .doOnSuccess(response -> 
                        log.info("Created payment intent: {}, order: {}", paymentNo, request.getOrderNo()));
    }

    /**
     * Get payment by ID
     */
    public Mono<PaymentResponse> getPayment(Long id) {
        return paymentRepository.findByIdAndIsValid(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Payment not found")))
                .map(PaymentResponse::fromEntity);
    }

    /**
     * Get payment by payment number
     */
    public Mono<PaymentResponse> getPaymentByNo(String paymentNo) {
        return paymentRepository.findByPaymentNoAndIsValid(paymentNo)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Payment not found")))
                .map(PaymentResponse::fromEntity);
    }

    /**
     * Get payments by order
     */
    public Flux<PaymentResponse> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderIdAndIsValid(orderId)
                .map(PaymentResponse::fromEntity);
    }

    /**
     * Get payments by customer
     */
    public Flux<PaymentResponse> getPaymentsByCustomer(Long customerId, int page, int size) {
        return paymentRepository.findByCustomerIdAndIsValidOrderByCreateTimeDesc(customerId)
                .skip((long) page * size)
                .take(size)
                .map(PaymentResponse::fromEntity);
    }

    /**
     * Confirm payment (from frontend after customer completes payment)
     */
    @Transactional
    public Mono<PaymentResponse> confirmPayment(Long paymentId, String paymentMethodId) {
        return paymentRepository.findByIdAndIsValid(paymentId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Payment not found")))
                .flatMap(payment -> {
                    if (!payment.isPending()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                "Payment is not in pending state"));
                    }

                    if (payment.isExpired()) {
                        payment.setPaymentStatus(Payment.PaymentStatus.CANCELLED);
                        return paymentRepository.save(payment)
                                .flatMap(p -> Mono.error(new BusinessException(
                                        ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                        "Payment has expired")));
                    }

                    if (payment.getChannel() == Payment.PaymentChannel.STRIPE) {
                        return stripeProvider.confirmPayment(payment.getThirdPartyTxnId(), paymentMethodId)
                                .flatMap(result -> {
                                    if (result.success()) {
                                        payment.markAsPaid(result.transactionId());
                                        payment.setPaymentMethodType(result.paymentMethodType());
                                        payment.setPaymentMethodLast4(result.paymentMethodLast4());
                                        payment.setPaymentMethodBrand(result.paymentMethodBrand());
                                        payment.setThirdPartyTxnData(result.rawResponse());
                                    } else {
                                        payment.markAsFailed(result.errorCode(), result.errorMessage());
                                    }
                                    return paymentRepository.save(payment)
                                            .map(PaymentResponse::fromEntity);
                                });
                    }

                    return Mono.just(PaymentResponse.fromEntity(payment));
                })
                .doOnSuccess(response -> 
                        log.info("Confirmed payment: {}", paymentId));
    }

    /**
     * Cancel payment
     */
    @Transactional
    public Mono<PaymentResponse> cancelPayment(Long paymentId) {
        return paymentRepository.findByIdAndIsValid(paymentId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Payment not found")))
                .flatMap(payment -> {
                    if (!payment.isPending()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                "Cannot cancel payment in state: " + payment.getPaymentStatus()));
                    }

                    payment.setPaymentStatus(Payment.PaymentStatus.CANCELLED);
                    return paymentRepository.save(payment)
                            .map(PaymentResponse::fromEntity);
                })
                .doOnSuccess(response -> 
                        log.info("Cancelled payment: {}", paymentId));
    }

    /**
     * Create refund
     */
    @Transactional
    public Mono<RefundResponse> createRefund(CreateRefundRequest request, String tenantId) {
        return paymentRepository.findByIdAndIsValid(request.getPaymentId())
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Payment not found")))
                .flatMap(payment -> {
                    if (!payment.canRefund()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                "Payment cannot be refunded in state: " + payment.getPaymentStatus()));
                    }

                    BigDecimal refundable = payment.getRefundableAmount();
                    if (request.getAmount().compareTo(refundable) > 0) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                "Refund amount exceeds refundable amount: " + refundable));
                    }

                    String refundNo = generateRefundNo();

                    Refund refund = Refund.builder()
                            .refundNo(refundNo)
                            .paymentId(payment.getId())
                            .orderId(payment.getOrderId())
                            .amount(request.getAmount())
                            .currency(payment.getCurrency())
                            .refundStatus(Refund.RefundStatus.PENDING)
                            .reasonType(Refund.RefundReason.valueOf(request.getReasonType()))
                            .reasonDescription(request.getReasonDescription())
                            .tenantId(tenantId)
                            .build();

                    return refundRepository.save(refund)
                            .flatMap(savedRefund -> {
                                if (payment.getChannel() == Payment.PaymentChannel.STRIPE) {
                                    return stripeProvider.refund(
                                            payment.getThirdPartyTxnId(),
                                            request.getAmount(),
                                            request.getReasonDescription()
                                    ).flatMap(result -> {
                                        if (result.success()) {
                                            savedRefund.markAsSucceeded(result.refundId());
                                            savedRefund.setThirdPartyRefundData(result.rawResponse());
                                            
                                            // Update payment refund amount
                                            BigDecimal newRefundedAmount = 
                                                payment.getRefundedAmount() != null ? 
                                                    payment.getRefundedAmount().add(request.getAmount()) :
                                                    request.getAmount();
                                            payment.addRefundAmount(request.getAmount());
                                            
                                            return refundRepository.save(savedRefund)
                                                    .then(paymentRepository.addRefundAmount(
                                                            payment.getId(), 
                                                            request.getAmount(),
                                                            payment.getPaymentStatus().name()))
                                                    .thenReturn(RefundResponse.fromEntity(savedRefund));
                                        } else {
                                            savedRefund.markAsFailed(result.errorMessage());
                                            return refundRepository.save(savedRefund)
                                                    .flatMap(r -> Mono.error(new BusinessException(
                                                            ErrorCode.BAD_REQUEST,
                                                            HttpStatus.BAD_REQUEST,
                                                            "Refund failed: " + result.errorMessage())));
                                        }
                                    });
                                }
                                return Mono.just(RefundResponse.fromEntity(savedRefund));
                            });
                })
                .doOnSuccess(response -> 
                        log.info("Created refund: {} for payment: {}", 
                                response.getRefundNo(), request.getPaymentId()));
    }

    /**
     * Get refund by ID
     */
    public Mono<RefundResponse> getRefund(Long id) {
        return refundRepository.findByIdAndIsValid(id)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Refund not found")))
                .map(RefundResponse::fromEntity);
    }

    /**
     * Get refunds by payment
     */
    public Flux<RefundResponse> getRefundsByPayment(Long paymentId) {
        return refundRepository.findByPaymentIdAndIsValidOrderByCreateTimeDesc(paymentId)
                .map(RefundResponse::fromEntity);
    }

    /**
     * Handle Stripe webhook
     */
    @Transactional
    public Mono<Void> handleStripeWebhook(String payload, String signature) {
        return stripeProvider.processWebhook(payload, signature)
                .flatMap(event -> {
                    log.info("Processing Stripe webhook: {}", event.type());

                    switch (event.type()) {
                        case "payment_intent.succeeded" -> {
                            return handlePaymentSuccess(event.paymentIntentId(), event);
                        }
                        case "payment_intent.payment_failed" -> {
                            return handlePaymentFailure(event.paymentIntentId(), event);
                        }
                        case "charge.refunded", "refund.updated" -> {
                            return handleRefundUpdate(event);
                        }
                        default -> {
                            return Mono.empty();
                        }
                    }
                });
    }

    private Mono<Void> handlePaymentSuccess(String paymentIntentId, PaymentProvider.WebhookEvent event) {
        return paymentRepository.findByThirdPartyTxnId(paymentIntentId)
                .flatMap(payment -> {
                    if (payment.isPending() || payment.getPaymentStatus() == Payment.PaymentStatus.PROCESSING) {
                        payment.markAsPaid(paymentIntentId);
                        return paymentRepository.save(payment).then();
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("Payment succeeded: {}", paymentIntentId));
    }

    private Mono<Void> handlePaymentFailure(String paymentIntentId, PaymentProvider.WebhookEvent event) {
        String errorMessage = event.data().getOrDefault("error", "Unknown error").toString();
        return paymentRepository.findByThirdPartyTxnId(paymentIntentId)
                .flatMap(payment -> {
                    if (payment.isPending()) {
                        payment.markAsFailed("payment_failed", errorMessage);
                        return paymentRepository.save(payment).then();
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("Payment failed: {}, reason: {}", paymentIntentId, errorMessage));
    }

    private Mono<Void> handleRefundUpdate(PaymentProvider.WebhookEvent event) {
        String refundId = event.refundId();
        if (refundId == null) {
            return Mono.empty();
        }

        return refundRepository.findByThirdPartyRefundId(refundId)
                .flatMap(refund -> {
                    if ("succeeded".equals(event.status())) {
                        if (refund.getRefundStatus() != Refund.RefundStatus.SUCCESS) {
                            refund.markAsSucceeded(refundId);
                            return refundRepository.save(refund).then();
                        }
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.info("Refund updated: {}", refundId));
    }

    /**
     * Process expired payments (cron job)
     */
    @Transactional
    public Mono<Long> processExpiredPayments() {
        return paymentRepository.findByPaymentStatusAndExpiredAtBeforeAndIsValid(
                        Payment.PaymentStatus.PENDING, Instant.now())
                .flatMap(payment -> {
                    payment.setPaymentStatus(Payment.PaymentStatus.CANCELLED);
                    return paymentRepository.save(payment);
                })
                .count()
                .doOnSuccess(count -> {
                    if (count > 0) {
                        log.info("Cancelled {} expired payments", count);
                    }
                });
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private String generatePaymentNo() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private String generateRefundNo() {
        return "REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
