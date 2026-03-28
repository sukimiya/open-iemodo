package com.iemodo.payment.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment provider interface for different payment channels
 */
public interface PaymentProvider {

    /**
     * Get provider name
     */
    String getName();

    /**
     * Create a payment intent/charge
     * 
     * @param amount Payment amount
     * @param currency Currency code (e.g., USD, EUR)
     * @param paymentMethodId Payment method ID (for saved cards)
     * @param metadata Additional metadata
     * @param returnUrl Return URL for redirect-based payments
     * @return Payment intent data including client secret and ID
     */
    Mono<PaymentIntentResult> createPaymentIntent(
            BigDecimal amount,
            String currency,
            String paymentMethodId,
            Map<String, Object> metadata,
            String returnUrl
    );

    /**
     * Confirm a payment intent
     * 
     * @param paymentIntentId Payment intent ID
     * @param paymentMethodId Payment method ID
     * @return Confirmed payment result
     */
    Mono<PaymentResult> confirmPayment(String paymentIntentId, String paymentMethodId);

    /**
     * Retrieve payment status
     * 
     * @param paymentIntentId Payment intent ID
     * @return Payment result
     */
    Mono<PaymentResult> retrievePayment(String paymentIntentId);

    /**
     * Refund a payment
     * 
     * @param paymentIntentId Original payment intent ID
     * @param amount Refund amount (null for full refund)
     * @param reason Refund reason
     * @return Refund result
     */
    Mono<RefundResult> refund(String paymentIntentId, BigDecimal amount, String reason);

    /**
     * Process webhook payload
     * 
     * @param payload Raw webhook payload
     * @param signature Signature header for verification
     * @return Webhook event
     */
    Mono<WebhookEvent> processWebhook(String payload, String signature);

    /**
     * Payment intent creation result
     */
    record PaymentIntentResult(
            boolean success,
            String paymentIntentId,
            String clientSecret,
            String status,
            String errorCode,
            String errorMessage,
            Map<String, Object> rawResponse
    ) {}

    /**
     * Payment result
     */
    record PaymentResult(
            boolean success,
            String paymentIntentId,
            String status,
            String transactionId,
            String paymentMethodType,
            String paymentMethodLast4,
            String paymentMethodBrand,
            String receiptUrl,
            String errorCode,
            String errorMessage,
            Map<String, Object> rawResponse
    ) {}

    /**
     * Refund result
     */
    record RefundResult(
            boolean success,
            String refundId,
            String status,
            BigDecimal amount,
            String errorCode,
            String errorMessage,
            Map<String, Object> rawResponse
    ) {}

    /**
     * Webhook event
     */
    record WebhookEvent(
            String type,
            String eventId,
            String paymentIntentId,
            String refundId,
            String status,
            Map<String, Object> data
    ) {}
}
