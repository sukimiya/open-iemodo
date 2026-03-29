package com.iemodo.review.domain;

/**
 * Review moderation lifecycle.
 *
 * <pre>
 * PENDING ──► APPROVED ──► (visible to public)
 *         └──► REJECTED  (not visible)
 * </pre>
 */
public enum ReviewStatus {
    /** Awaiting moderation — not yet visible to other customers. */
    PENDING,
    /** Passed moderation — visible on the product page. */
    APPROVED,
    /** Failed moderation (spam, inappropriate content, etc.) */
    REJECTED
}
