package com.iemodo.payment.controller;

import com.iemodo.common.response.Response;
import com.iemodo.payment.dto.*;
import com.iemodo.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Payment controller
 */
@Slf4j
@RestController
@RequestMapping("/pay/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment intent
     */
    @PostMapping("/payments/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Response<PaymentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        log.info("Creating payment intent for order: {}, tenant: {}", request.getOrderNo(), tenantId);
        
        return paymentService.createPaymentIntent(request, tenantId)
                .map(Response::success);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/payments/{paymentId}")
    public Mono<Response<PaymentResponse>> getPayment(
            @PathVariable Long paymentId) {
        
        return paymentService.getPayment(paymentId)
                .map(Response::success);
    }

    /**
     * Get payment by payment number
     */
    @GetMapping("/payments/by-no/{paymentNo}")
    public Mono<Response<PaymentResponse>> getPaymentByNo(
            @PathVariable String paymentNo) {
        
        return paymentService.getPaymentByNo(paymentNo)
                .map(Response::success);
    }

    /**
     * Get payments by order
     */
    @GetMapping("/orders/{orderId}/payments")
    public Mono<Response<java.util.List<PaymentResponse>>> getPaymentsByOrder(
            @PathVariable Long orderId) {
        
        return paymentService.getPaymentsByOrder(orderId)
                .collectList()
                .map(Response::success);
    }

    /**
     * Confirm payment
     */
    @PostMapping("/payments/{paymentId}/confirm")
    public Mono<Response<PaymentResponse>> confirmPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String paymentMethodId) {
        
        return paymentService.confirmPayment(paymentId, paymentMethodId)
                .map(Response::success);
    }

    /**
     * Cancel payment
     */
    @PostMapping("/payments/{paymentId}/cancel")
    public Mono<Response<PaymentResponse>> cancelPayment(
            @PathVariable Long paymentId) {
        
        return paymentService.cancelPayment(paymentId)
                .map(Response::success);
    }

    /**
     * Create refund
     */
    @PostMapping("/payments/{paymentId}/refund")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Response<RefundResponse>> createRefund(
            @PathVariable Long paymentId,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        request.setPaymentId(paymentId);
        
        return paymentService.createRefund(request, tenantId)
                .map(Response::success);
    }

    /**
     * Get refund by ID
     */
    @GetMapping("/refunds/{refundId}")
    public Mono<Response<RefundResponse>> getRefund(
            @PathVariable Long refundId) {
        
        return paymentService.getRefund(refundId)
                .map(Response::success);
    }

    /**
     * Get refunds by payment
     */
    @GetMapping("/payments/{paymentId}/refunds")
    public Mono<Response<java.util.List<RefundResponse>>> getRefundsByPayment(
            @PathVariable Long paymentId) {
        
        return paymentService.getRefundsByPayment(paymentId)
                .collectList()
                .map(Response::success);
    }
}
