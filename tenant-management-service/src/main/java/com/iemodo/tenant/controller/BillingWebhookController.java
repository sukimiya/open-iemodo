package com.iemodo.tenant.controller;

import com.iemodo.common.response.Response;
import com.iemodo.tenant.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/webhook")
@RequiredArgsConstructor
public class BillingWebhookController {

    private final BillingService billingService;

    @PostMapping("/stripe")
    public Mono<ResponseEntity<Response<String>>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        return billingService.handleWebhook(payload, signature)
                .map(result -> ResponseEntity.ok(Response.success(result)))
                .onErrorResume(e -> {
                    log.error("Stripe webhook processing failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity
                            .badRequest()
                            .body(Response.error(400, "Webhook processing failed: " + e.getMessage())));
                });
    }
}
