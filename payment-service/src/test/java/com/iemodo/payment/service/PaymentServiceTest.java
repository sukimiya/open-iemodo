package com.iemodo.payment.service;

import com.iemodo.payment.domain.Payment;
import com.iemodo.payment.domain.Refund;
import com.iemodo.payment.dto.CreatePaymentRequest;
import com.iemodo.payment.dto.CreateRefundRequest;
import com.iemodo.payment.dto.PaymentResponse;
import com.iemodo.payment.dto.RefundResponse;
import com.iemodo.payment.repository.PaymentRepository;
import com.iemodo.payment.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Payment service unit tests
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private StripePaymentProvider stripeProvider;

    @InjectMocks
    private PaymentService paymentService;

    private Payment samplePayment;
    private Refund sampleRefund;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(1L)
                .paymentNo("PAY12345678901234567890")
                .orderId(1L)
                .orderNo("ORD123")
                .customerId(1L)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .channel(Payment.PaymentChannel.STRIPE)
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .expiredAt(Instant.now().plusSeconds(1800))
                .tenantId("tenant_001")
                .build();

        sampleRefund = Refund.builder()
                .id(1L)
                .refundNo("REF12345678901234567890")
                .paymentId(1L)
                .orderId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .refundStatus(Refund.RefundStatus.PENDING)
                .reasonType(Refund.RefundReason.CUSTOMER_REQUEST)
                .tenantId("tenant_001")
                .build();
    }

    @Test
    void createPaymentIntent_Success() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(1L)
                .orderNo("ORD123")
                .customerId(1L)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .channel("STRIPE")
                .build();

        PaymentProvider.PaymentIntentResult stripeResult = new PaymentProvider.PaymentIntentResult(
                true,
                "pi_test_123",
                "secret_123",
                "requires_confirmation",
                null,
                null,
                new HashMap<>()
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(Mono.just(samplePayment))
                .thenReturn(Mono.just(samplePayment));
        when(stripeProvider.createPaymentIntent(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(stripeResult));

        StepVerifier.create(paymentService.createPaymentIntent(request, "tenant_001"))
                .expectNextMatches(response -> 
                        response.getOrderNo().equals("ORD123") &&
                        response.getAmount().equals(new BigDecimal("99.99")) &&
                        response.getCurrency().equals("USD"))
                .verifyComplete();
    }

    @Test
    void getPayment_Success() {
        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));

        StepVerifier.create(paymentService.getPayment(1L))
                .expectNextMatches(response ->
                        response.getId().equals(1L) &&
                        response.getPaymentNo().equals("PAY12345678901234567890") &&
                        response.getStatus().equals("PENDING"))
                .verifyComplete();
    }

    @Test
    void getPayment_NotFound() {
        when(paymentRepository.findByIdAndIsValid(999L)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getPayment(999L))
                .expectErrorMessage("Payment not found")
                .verify();
    }

    @Test
    void cancelPayment_Success() {
        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(samplePayment));

        StepVerifier.create(paymentService.cancelPayment(1L))
                .expectNextMatches(response ->
                        response.getStatus().equals("CANCELLED"))
                .verifyComplete();
    }

    @Test
    void createRefund_Success() {
        samplePayment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        samplePayment.setRefundedAmount(BigDecimal.ZERO);

        CreateRefundRequest request = CreateRefundRequest.builder()
                .paymentId(1L)
                .amount(new BigDecimal("50.00"))
                .reasonType("CUSTOMER_REQUEST")
                .build();

        PaymentProvider.RefundResult stripeResult = new PaymentProvider.RefundResult(
                true,
                "re_test_123",
                "succeeded",
                new BigDecimal("50.00"),
                null,
                null,
                new HashMap<>()
        );

        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));
        when(refundRepository.save(any(Refund.class)))
                .thenReturn(Mono.just(sampleRefund))
                .thenReturn(Mono.just(sampleRefund));
        when(stripeProvider.refund(any(), any(), any())).thenReturn(Mono.just(stripeResult));
        when(paymentRepository.addRefundAmount(anyLong(), any(), any())).thenReturn(Mono.just(1));

        StepVerifier.create(paymentService.createRefund(request, "tenant_001"))
                .expectNextMatches(response ->
                        response.getAmount().equals(new BigDecimal("50.00")) &&
                        response.getStatus().equals("SUCCESS"))
                .verifyComplete();
    }

    @Test
    void createRefund_ExceedsRefundableAmount() {
        samplePayment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        samplePayment.setRefundedAmount(BigDecimal.ZERO);
        samplePayment.setAmount(new BigDecimal("30.00"));

        CreateRefundRequest request = CreateRefundRequest.builder()
                .paymentId(1L)
                .amount(new BigDecimal("50.00"))
                .reasonType("CUSTOMER_REQUEST")
                .build();

        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));

        StepVerifier.create(paymentService.createRefund(request, "tenant_001"))
                .expectError()
                .verify();
    }

    @Test
    void createRefund_PaymentNotRefundable() {
        samplePayment.setPaymentStatus(Payment.PaymentStatus.PENDING);

        CreateRefundRequest request = CreateRefundRequest.builder()
                .paymentId(1L)
                .amount(new BigDecimal("50.00"))
                .reasonType("CUSTOMER_REQUEST")
                .build();

        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));

        StepVerifier.create(paymentService.createRefund(request, "tenant_001"))
                .expectError()
                .verify();
    }

    @Test
    void getRefund_Success() {
        when(refundRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(sampleRefund));

        StepVerifier.create(paymentService.getRefund(1L))
                .expectNextMatches(response ->
                        response.getId().equals(1L) &&
                        response.getRefundNo().equals("REF12345678901234567890"))
                .verifyComplete();
    }

    @Test
    void confirmPayment_Expired() {
        samplePayment.setExpiredAt(Instant.now().minusSeconds(60));

        when(paymentRepository.findByIdAndIsValid(1L)).thenReturn(Mono.just(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(Mono.just(samplePayment));

        StepVerifier.create(paymentService.confirmPayment(1L, "pm_test"))
                .expectError()
                .verify();
    }
}
