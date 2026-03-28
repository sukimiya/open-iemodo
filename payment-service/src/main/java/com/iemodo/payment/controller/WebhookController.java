package com.iemodo.payment.controller;

import com.iemodo.common.response.Response;
import com.iemodo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
     */
    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Response<String>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Received Stripe webhook");
        
        return paymentService.handleStripeWebhook(payload, signature)
                .thenReturn(Response.success("OK"))
                .onErrorResume(e -> {
                    log.error("Stripe webhook error: {}", e.getMessage());
                    return Mono.just(Response.error(400, "Webhook processing failed: " + e.getMessage()));
                });
    }
}
