package com.iemodo.review.controller;

import com.iemodo.common.response.Response;
import com.iemodo.review.dto.CreateReviewRequest;
import com.iemodo.review.dto.ProductRatingDTO;
import com.iemodo.review.dto.ReviewDTO;
import com.iemodo.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/review/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ─── Customer endpoints ───────────────────────────────────────────────

    @PostMapping("/reviews")
    public Mono<Response<ReviewDTO>> submit(
            @Valid @RequestBody CreateReviewRequest req,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-TenantID") String tenantId) {
        return reviewService.submit(req, userId, tenantId)
                .map(Response::success);
    }

    @GetMapping("/products/{productId}/reviews")
    public Flux<ReviewDTO> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reviewService.listByProduct(productId, page, size);
    }

    @GetMapping("/products/{productId}/rating")
    public Mono<Response<ProductRatingDTO>> getRating(
            @PathVariable Long productId,
            @RequestHeader("X-TenantID") String tenantId) {
        return reviewService.getRating(productId, tenantId)
                .map(Response::success);
    }

    @GetMapping("/users/me/reviews")
    public Flux<ReviewDTO> myReviews(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reviewService.listByUser(userId, page, size);
    }

    @PostMapping("/reviews/{reviewId}/helpful")
    public Mono<Response<Void>> markHelpful(@PathVariable Long reviewId) {
        return reviewService.markHelpful(reviewId)
                .then(Mono.just(Response.success()));
    }

    // ─── Merchant endpoints ───────────────────────────────────────────────

    @PostMapping("/reviews/{reviewId}/replies")
    public Mono<Response<ReviewDTO>> reply(
            @PathVariable Long reviewId,
            @RequestParam String content,
            @RequestParam(defaultValue = "MERCHANT") String replierType,
            @RequestHeader("X-User-Id") Long replierId) {
        return reviewService.reply(reviewId, replierId, replierType, content)
                .map(Response::success);
    }

    // ─── Admin / moderation endpoints ────────────────────────────────────

    @GetMapping("/admin/reviews/pending")
    public Flux<ReviewDTO> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listPendingModeration(page, size);
    }

    @PutMapping("/admin/reviews/{reviewId}/approve")
    public Mono<Response<ReviewDTO>> approve(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long operatorId) {
        return reviewService.approve(reviewId, operatorId)
                .map(Response::success);
    }

    @PutMapping("/admin/reviews/{reviewId}/reject")
    public Mono<Response<ReviewDTO>> reject(
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long operatorId) {
        return reviewService.reject(reviewId, operatorId)
                .map(Response::success);
    }
}
