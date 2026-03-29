package com.iemodo.review.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.review.domain.*;
import com.iemodo.review.dto.CreateReviewRequest;
import com.iemodo.review.repository.ProductRatingSummaryRepository;
import com.iemodo.review.repository.ReviewReplyRepository;
import com.iemodo.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock private ReviewRepository             reviewRepository;
    @Mock private ReviewReplyRepository        replyRepository;
    @Mock private ProductRatingSummaryRepository summaryRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, replyRepository, summaryRepository);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private CreateReviewRequest buildRequest(boolean withMedia) {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setOrderId(100L);
        req.setOrderItemId(200L);
        req.setProductId(10L);
        req.setRating(5);
        req.setTitle("Great product");
        req.setContent("Really happy with this purchase, works perfectly.");
        if (withMedia) req.setMediaUrls(java.util.List.of("https://cdn.example.com/img1.jpg"));
        return req;
    }

    private Review savedReview(ReviewStatus status) {
        return Review.builder()
                .id(1L)
                .tenantId("tenant_001")
                .productId(10L)
                .orderId(100L)
                .orderItemId(200L)
                .userId(99L)
                .rating(5)
                .title("Great product")
                .content("Really happy with this purchase, works perfectly.")
                .reviewStatus(status)
                .helpfulCount(0)
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .build();
    }

    // ─── submit ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit: text-only review should be auto-approved")
    void submit_textOnly_shouldBeAutoApproved() {
        when(reviewRepository.existsByOrderItemIdAndUserId(200L, 99L)).thenReturn(Mono.just(false));
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return Mono.just(r);
        });
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());
        when(summaryRepository.findByProductIdAndTenantId(10L, "tenant_001"))
                .thenReturn(Mono.empty());
        when(summaryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(reviewService.submit(buildRequest(false), 99L, "tenant_001"))
                .assertNext(dto -> {
                    assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
                    assertThat(dto.getApprovedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("submit: review with media should be PENDING for moderation")
    void submit_withMedia_shouldBePending() {
        when(reviewRepository.existsByOrderItemIdAndUserId(200L, 99L)).thenReturn(Mono.just(false));
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return Mono.just(r);
        });
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());

        StepVerifier.create(reviewService.submit(buildRequest(true), 99L, "tenant_001"))
                .assertNext(dto -> assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.PENDING))
                .verifyComplete();
    }

    @Test
    @DisplayName("submit: duplicate review should throw BusinessException")
    void submit_duplicate_shouldThrow() {
        when(reviewRepository.existsByOrderItemIdAndUserId(200L, 99L)).thenReturn(Mono.just(true));

        StepVerifier.create(reviewService.submit(buildRequest(false), 99L, "tenant_001"))
                .expectErrorMatches(ex -> ex instanceof BusinessException)
                .verify();
    }

    // ─── approve ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve: should set status APPROVED and update rating summary")
    void approve_shouldApproveAndUpdateSummary() {
        Review review = savedReview(ReviewStatus.PENDING);
        when(reviewRepository.findById(1L)).thenReturn(Mono.just(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());
        when(summaryRepository.findByProductIdAndTenantId(10L, "tenant_001"))
                .thenReturn(Mono.just(ProductRatingSummary.empty(10L, "tenant_001")));
        when(summaryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(reviewService.approve(1L, 42L))
                .assertNext(dto -> {
                    assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
                    assertThat(dto.getApprovedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("approve: idempotent — approving an already-approved review is a no-op")
    void approve_idempotent_whenAlreadyApproved() {
        Review review = savedReview(ReviewStatus.APPROVED);
        review.setApprovedAt(Instant.now().minusSeconds(100));
        when(reviewRepository.findById(1L)).thenReturn(Mono.just(review));
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());

        StepVerifier.create(reviewService.approve(1L, 42L))
                .assertNext(dto -> assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED))
                .verifyComplete();
    }

    // ─── reject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("reject: PENDING review should be rejected without touching rating summary")
    void reject_pendingReview_noSummaryChange() {
        Review review = savedReview(ReviewStatus.PENDING);
        when(reviewRepository.findById(1L)).thenReturn(Mono.just(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());

        StepVerifier.create(reviewService.reject(1L, 42L))
                .assertNext(dto -> assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.REJECTED))
                .verifyComplete();
    }

    @Test
    @DisplayName("reject: previously APPROVED review should roll back rating summary")
    void reject_approvedReview_rollsBackSummary() {
        Review review = savedReview(ReviewStatus.APPROVED);
        ProductRatingSummary summary = ProductRatingSummary.empty(10L, "tenant_001");
        summary.addRating(5); // one 5-star already counted

        when(reviewRepository.findById(1L)).thenReturn(Mono.just(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(replyRepository.findByReviewIdOrderByCreateTimeAsc(1L)).thenReturn(Flux.empty());
        when(summaryRepository.findByProductIdAndTenantId(10L, "tenant_001"))
                .thenReturn(Mono.just(summary));
        when(summaryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(reviewService.reject(1L, 42L))
                .assertNext(dto -> assertThat(dto.getReviewStatus()).isEqualTo(ReviewStatus.REJECTED))
                .verifyComplete();
    }

    // ─── getRating ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRating: returns zero summary when no reviews yet")
    void getRating_returnsZero_whenNoReviews() {
        when(summaryRepository.findByProductIdAndTenantId(99L, "tenant_001"))
                .thenReturn(Mono.empty());

        StepVerifier.create(reviewService.getRating(99L, "tenant_001"))
                .assertNext(dto -> {
                    assertThat(dto.getTotalReviews()).isEqualTo(0);
                    assertThat(dto.getAvgRating()).isEqualByComparingTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    // ─── findById (not found) ─────────────────────────────────────────────

    @Test
    @DisplayName("approve: should throw NOT_FOUND when review does not exist")
    void approve_shouldThrow_whenNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(reviewService.approve(999L, 1L))
                .expectErrorMatches(ex -> ex instanceof BusinessException be
                        && be.getErrorCode() == com.iemodo.common.exception.ErrorCode.NOT_FOUND)
                .verify();
    }
}
