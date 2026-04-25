package com.iemodo.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment provider implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isEmpty()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe payment provider initialized");
        } else {
            log.warn("Stripe secret key not configured");
        }
    }

    @Override
    public String getName() {
        return "STRIPE";
    }

    @Override
    public Mono<PaymentIntentResult> createPaymentIntent(
            BigDecimal amount,
            String currency,
            String paymentMethodId,
            Map<String, Object> metadata,
            String returnUrl) {
        
        return Mono.fromCallable(() -> {
            // Convert amount to cents/smallest currency unit
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                paramsBuilder.setPaymentMethod(paymentMethodId);
            }

            if (metadata != null) {
                metadata.forEach((key, value) -> {
                    if (value instanceof String) {
                        paramsBuilder.putMetadata(key, (String) value);
                    }
                });
            }

            paramsBuilder.putExtraParam("idempotency_key", java.util.UUID.randomUUID().toString());
            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            return new PaymentIntentResult(
                    true,
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    paymentIntent.getStatus(),
                    null,
                    null,
                    Map.of("id", paymentIntent.getId(), "status", paymentIntent.getStatus())
            );
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(StripeException.class, e -> {
              log.error("Stripe error creating payment intent: {}", e.getMessage());
              return Mono.just(new PaymentIntentResult(
                      false,
                      null,
                      null,
                      null,
                      e.getCode(),
                      e.getMessage(),
                      Map.of("error", e.getMessage())
              ));
          });
    }

    @Override
    public Mono<PaymentResult> confirmPayment(String paymentIntentId, String paymentMethodId) {
        return Mono.fromCallable(() -> {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            PaymentIntent confirmedIntent;
            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .build();
                confirmedIntent = paymentIntent.confirm(confirmParams);
            } else {
                confirmedIntent = paymentIntent;
            }

            return mapToPaymentResult(confirmedIntent);
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(StripeException.class, e -> {
              log.error("Stripe error confirming payment: {}", e.getMessage());
              return Mono.just(new PaymentResult(
                      false,
                      paymentIntentId,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      e.getCode(),
                      e.getMessage(),
                      Map.of("error", e.getMessage())
              ));
          });
    }

    @Override
    public Mono<PaymentResult> retrievePayment(String paymentIntentId) {
        return Mono.fromCallable(() -> {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return mapToPaymentResult(paymentIntent);
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(StripeException.class, e -> {
              log.error("Stripe error retrieving payment: {}", e.getMessage());
              return Mono.just(new PaymentResult(
                      false,
                      paymentIntentId,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      e.getCode(),
                      e.getMessage(),
                      Map.of("error", e.getMessage())
              ));
          });
    }

    @Override
    public Mono<RefundResult> refund(String paymentIntentId, BigDecimal amount, String reason) {
        return Mono.fromCallable(() -> {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                paramsBuilder.setAmount(amountInCents);
            }

            if (reason != null && !reason.isEmpty()) {
                paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
            }

            paramsBuilder.putExtraParam("idempotency_key", java.util.UUID.randomUUID().toString());
            Refund refund = Refund.create(paramsBuilder.build());

            BigDecimal refundAmount = BigDecimal.valueOf(refund.getAmount())
                    .divide(BigDecimal.valueOf(100));

            return new RefundResult(
                    "succeeded".equals(refund.getStatus()),
                    refund.getId(),
                    refund.getStatus(),
                    refundAmount,
                    null,
                    null,
                    Map.of("id", refund.getId(), "status", refund.getStatus())
            );
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(StripeException.class, e -> {
              log.error("Stripe error creating refund: {}", e.getMessage());
              return Mono.just(new RefundResult(
                      false,
                      null,
                      null,
                      amount,
                      e.getCode(),
                      e.getMessage(),
                      Map.of("error", e.getMessage())
              ));
          });
    }

    @Override
    public Mono<WebhookEvent> processWebhook(String payload, String signature) {
        return Mono.fromCallable(() -> {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            String paymentIntentId = null;
            String refundId = null;
            String status = null;
            Map<String, Object> data = new HashMap<>();

            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (pi != null) {
                        paymentIntentId = pi.getId();
                        status = "succeeded";
                        data.put("amount", pi.getAmount());
                        data.put("currency", pi.getCurrency());
                    }
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (pi != null) {
                        paymentIntentId = pi.getId();
                        status = "failed";
                        data.put("error", pi.getLastPaymentError() != null ? 
                                pi.getLastPaymentError().getMessage() : "Unknown error");
                    }
                }
                case "charge.refunded" -> {
                    com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (charge != null) {
                        paymentIntentId = charge.getPaymentIntent();
                        status = "refunded";
                        data.put("amount_refunded", charge.getAmountRefunded());
                    }
                }
                case "refund.created" -> {
                    Refund refund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (refund != null) {
                        refundId = refund.getId();
                        paymentIntentId = refund.getPaymentIntent();
                        status = refund.getStatus();
                        data.put("amount", refund.getAmount());
                    }
                }
                case "refund.updated" -> {
                    Refund refund = (Refund) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (refund != null) {
                        refundId = refund.getId();
                        paymentIntentId = refund.getPaymentIntent();
                        status = refund.getStatus();
                    }
                }
            }

            return new WebhookEvent(
                    event.getType(),
                    event.getId(),
                    paymentIntentId,
                    refundId,
                    status,
                    data
            );
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              log.error("Stripe webhook processing error: {}", e.getMessage());
              return Mono.error(new RuntimeException("Invalid webhook payload", e));
          });
    }

    private PaymentResult mapToPaymentResult(PaymentIntent paymentIntent) {
        String paymentMethodType = null;
        String last4 = null;
        String brand = null;
        String receiptUrl = null;

        // In newer Stripe SDK versions, charges might be accessed differently
        // or might be null until the payment is confirmed
        try {
            if (paymentIntent.getLatestCharge() != null) {
                com.stripe.model.Charge charge = com.stripe.model.Charge.retrieve(paymentIntent.getLatestCharge());
                if (charge != null) {
                    receiptUrl = charge.getReceiptUrl();
                    
                    if (charge.getPaymentMethodDetails() != null) {
                        paymentMethodType = charge.getPaymentMethodDetails().getType();
                        
                        if (charge.getPaymentMethodDetails().getCard() != null) {
                            var card = charge.getPaymentMethodDetails().getCard();
                            last4 = card.getLast4();
                            brand = card.getNetwork();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve charge details: {}", e.getMessage());
        }

        boolean success = "succeeded".equals(paymentIntent.getStatus());

        return new PaymentResult(
                success,
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getId(),
                paymentMethodType,
                last4,
                brand,
                receiptUrl,
                null,
                null,
                Map.of(
                        "id", paymentIntent.getId(),
                        "status", paymentIntent.getStatus(),
                        "amount", paymentIntent.getAmount()
                )
        );
    }
}
