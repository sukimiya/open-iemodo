package com.iemodo.payment.controller;

import com.iemodo.common.response.Response;
import com.iemodo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Webhook controller for payment provider callbacks
 */
@Slf4j
@RestController
@RequestMapping("/pay/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Stripe webhook endpoint
     *
     * <p>Returns 200 on success. Returns 400 on signature verification failure
     * or processing error (Stripe will retry with exponential backoff).
     */
    @PostMapping("/stripe")
    public Mono<ResponseEntity<Response<String>>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook");

        return paymentService.handleStripeWebhook(payload, signature)
                .thenReturn(ResponseEntity.ok(Response.success("OK")))
                .onErrorResume(e -> {
                    log.error("Stripe webhook error: {}", e.getMessage());
                    return Mono.just(ResponseEntity
                            .badRequest()
                            .body(Response.error(400, "Webhook processing failed: " + e.getMessage())));
                });
    }
}
