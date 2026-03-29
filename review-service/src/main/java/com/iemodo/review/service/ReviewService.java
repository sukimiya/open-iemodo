package com.iemodo.review.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.review.domain.*;
import com.iemodo.review.dto.CreateReviewRequest;
import com.iemodo.review.dto.ProductRatingDTO;
import com.iemodo.review.dto.ReviewDTO;
import com.iemodo.review.repository.ProductRatingSummaryRepository;
import com.iemodo.review.repository.ReviewReplyRepository;
import com.iemodo.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository             reviewRepository;
    private final ReviewReplyRepository        replyRepository;
    private final ProductRatingSummaryRepository summaryRepository;

    // ─── Submit review ────────────────────────────────────────────────────

    @Transactional
    public Mono<ReviewDTO> submit(CreateReviewRequest req, Long userId, String tenantId) {
        // Guard: one review per purchased order item
        return reviewRepository.existsByOrderItemIdAndUserId(req.getOrderItemId(), userId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT,
                                "You have already reviewed this item"));
                    }

                    String mediaUrlsCsv = req.getMediaUrls() == null ? null
                            : String.join(",", req.getMediaUrls());

                    // Reviews with media require manual moderation; text-only auto-approve
                    boolean autoApprove = (req.getMediaUrls() == null || req.getMediaUrls().isEmpty())
                            && req.getContent() != null
                            && req.getContent().length() >= 5;

                    Review review = Review.builder()
                            .tenantId(tenantId)
                            .productId(req.getProductId())
                            .skuId(req.getSkuId())
                            .orderId(req.getOrderId())
                            .orderItemId(req.getOrderItemId())
                            .userId(userId)
                            .rating(req.getRating())
                            .title(req.getTitle())
                            .content(req.getContent())
                            .mediaUrls(mediaUrlsCsv)
                            .reviewStatus(autoApprove ? ReviewStatus.APPROVED : ReviewStatus.PENDING)
                            .helpfulCount(0)
                            .approvedAt(autoApprove ? Instant.now() : null)
                            .build();

                    return reviewRepository.save(review);
                })
                .flatMap(saved -> {
                    // If auto-approved, update rating summary immediately
                    if (saved.getReviewStatus() == ReviewStatus.APPROVED) {
                        return updateRatingSummary(saved.getProductId(), tenantId, saved.getRating(), true)
                                .thenReturn(saved);
                    }
                    return Mono.just(saved);
                })
                .flatMap(this::loadRepliesAndToDTO)
                .doOnSuccess(dto -> log.info("Review submitted: product={} user={} status={}",
                        dto.getProductId(), userId, dto.getReviewStatus()));
    }

    // ─── Moderation ───────────────────────────────────────────────────────

    @Transactional
    public Mono<ReviewDTO> approve(Long reviewId, Long operatorId) {
        return findById(reviewId)
                .flatMap(review -> {
                    if (review.getReviewStatus() == ReviewStatus.APPROVED) {
                        return Mono.just(review); // idempotent
                    }
                    review.setReviewStatus(ReviewStatus.APPROVED);
                    review.setApprovedAt(Instant.now());
                    return reviewRepository.save(review)
                            .flatMap(saved ->
                                    updateRatingSummary(saved.getProductId(),
                                            saved.getTenantId(), saved.getRating(), true)
                                            .thenReturn(saved));
                })
                .flatMap(this::loadRepliesAndToDTO)
                .doOnSuccess(dto -> log.info("Review={} approved by operator={}", reviewId, operatorId));
    }

    @Transactional
    public Mono<ReviewDTO> reject(Long reviewId, Long operatorId) {
        return findById(reviewId)
                .flatMap(review -> {
                    // If it was previously approved, roll back the rating summary
                    boolean wasApproved = review.getReviewStatus() == ReviewStatus.APPROVED;
                    review.setReviewStatus(ReviewStatus.REJECTED);
                    return reviewRepository.save(review)
                            .flatMap(saved -> {
                                if (wasApproved) {
                                    return updateRatingSummary(saved.getProductId(),
                                            saved.getTenantId(), saved.getRating(), false)
                                            .thenReturn(saved);
                                }
                                return Mono.just(saved);
                            });
                })
                .flatMap(this::loadRepliesAndToDTO)
                .doOnSuccess(dto -> log.info("Review={} rejected by operator={}", reviewId, operatorId));
    }

    // ─── Merchant reply ───────────────────────────────────────────────────

    @Transactional
    public Mono<ReviewDTO> reply(Long reviewId, Long replierId,
                                  String replierType, String content) {
        return findById(reviewId)
                .flatMap(review -> {
                    ReviewReply rep = ReviewReply.builder()
                            .reviewId(reviewId)
                            .replierId(replierId)
                            .replierType(replierType)
                            .content(content)
                            .build();
                    return replyRepository.save(rep).thenReturn(review);
                })
                .flatMap(this::loadRepliesAndToDTO);
    }

    // ─── Mark helpful ─────────────────────────────────────────────────────

    public Mono<Void> markHelpful(Long reviewId) {
        return reviewRepository.incrementHelpfulCount(reviewId).then();
    }

    // ─── Query ────────────────────────────────────────────────────────────

    public Flux<ReviewDTO> listByProduct(Long productId, int page, int size) {
        return reviewRepository.findByProductIdAndReviewStatusOrderByCreateTimeDesc(
                        productId, ReviewStatus.APPROVED, PageRequest.of(page, size))
                .flatMap(this::loadRepliesAndToDTO);
    }

    public Flux<ReviewDTO> listByUser(Long userId, int page, int size) {
        return reviewRepository.findByUserIdOrderByCreateTimeDesc(
                        userId, PageRequest.of(page, size))
                .flatMap(this::loadRepliesAndToDTO);
    }

    public Flux<ReviewDTO> listPendingModeration(int page, int size) {
        return reviewRepository.findByReviewStatusOrderByCreateTimeAsc(
                        ReviewStatus.PENDING, PageRequest.of(page, size))
                .flatMap(this::loadRepliesAndToDTO);
    }

    public Mono<ProductRatingDTO> getRating(Long productId, String tenantId) {
        return summaryRepository.findByProductIdAndTenantId(productId, tenantId)
                .map(this::toRatingDTO)
                .switchIfEmpty(Mono.just(ProductRatingDTO.builder()
                        .productId(productId)
                        .avgRating(java.math.BigDecimal.ZERO)
                        .totalReviews(0)
                        .fiveStar(0).fourStar(0).threeStar(0).twoStar(0).oneStar(0)
                        .build()));
    }

    // ─── Rating summary update ────────────────────────────────────────────

    private Mono<ProductRatingSummary> updateRatingSummary(Long productId, String tenantId,
                                                            int stars, boolean add) {
        return summaryRepository.findByProductIdAndTenantId(productId, tenantId)
                .switchIfEmpty(Mono.just(ProductRatingSummary.empty(productId, tenantId)))
                .map(summary -> {
                    if (add) summary.addRating(stars);
                    else     summary.removeRating(stars);
                    return summary;
                })
                .flatMap(summaryRepository::save);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private Mono<Review> findById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Review not found: " + reviewId)));
    }

    private Mono<ReviewDTO> loadRepliesAndToDTO(Review review) {
        return replyRepository.findByReviewIdOrderByCreateTimeAsc(review.getId())
                .collectList()
                .map(replies -> {
                    review.setReplies(replies);
                    return toDTO(review);
                });
    }

    private ReviewDTO toDTO(Review review) {
        List<ReviewDTO.ReviewReplyDTO> replyDTOs = review.getReplies() == null ? List.of() :
                review.getReplies().stream()
                        .map(r -> ReviewDTO.ReviewReplyDTO.builder()
                                .id(r.getId())
                                .replierId(r.getReplierId())
                                .replierType(r.getReplierType())
                                .content(r.getContent())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        List<String> mediaList = review.getMediaUrls() == null || review.getMediaUrls().isBlank()
                ? List.of()
                : Arrays.asList(review.getMediaUrls().split(","));

        return ReviewDTO.builder()
                .id(review.getId())
                .tenantId(review.getTenantId())
                .productId(review.getProductId())
                .orderId(review.getOrderId())
                .orderItemId(review.getOrderItemId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .mediaUrls(mediaList)
                .reviewStatus(review.getReviewStatus())
                .helpfulCount(review.getHelpfulCount())
                .approvedAt(review.getApprovedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .replies(replyDTOs)
                .build();
    }

    private ProductRatingDTO toRatingDTO(ProductRatingSummary s) {
        return ProductRatingDTO.builder()
                .productId(s.getProductId())
                .avgRating(s.getAvgRating())
                .totalReviews(s.getTotalReviews())
                .fiveStar(s.getFiveStar())
                .fourStar(s.getFourStar())
                .threeStar(s.getThreeStar())
                .twoStar(s.getTwoStar())
                .oneStar(s.getOneStar())
                .build();
    }
}
