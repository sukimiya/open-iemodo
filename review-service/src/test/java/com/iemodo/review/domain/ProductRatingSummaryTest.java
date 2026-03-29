package com.iemodo.review.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductRatingSummary")
class ProductRatingSummaryTest {

    @Test
    @DisplayName("addRating: correctly increments star bucket and recalculates average")
    void addRating_updatesAverage() {
        ProductRatingSummary summary = ProductRatingSummary.empty(1L, "tenant_001");

        summary.addRating(5);
        summary.addRating(5);
        summary.addRating(3);

        assertThat(summary.getTotalReviews()).isEqualTo(3);
        assertThat(summary.getFiveStar()).isEqualTo(2);
        assertThat(summary.getThreeStar()).isEqualTo(1);
        // avg = (5+5+3) / 3 = 4.33
        assertThat(summary.getAvgRating()).isEqualByComparingTo("4.33");
    }

    @Test
    @DisplayName("removeRating: correctly decrements star bucket and recalculates average")
    void removeRating_updatesAverage() {
        ProductRatingSummary summary = ProductRatingSummary.empty(1L, "tenant_001");
        summary.addRating(5);
        summary.addRating(5);
        summary.addRating(1);

        // Remove the 1-star rating
        summary.removeRating(1);

        assertThat(summary.getTotalReviews()).isEqualTo(2);
        assertThat(summary.getOneStar()).isEqualTo(0);
        assertThat(summary.getAvgRating()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("removeRating: star bucket never goes below zero")
    void removeRating_neverNegative() {
        ProductRatingSummary summary = ProductRatingSummary.empty(1L, "tenant_001");
        summary.removeRating(5); // removing from empty summary

        assertThat(summary.getFiveStar()).isEqualTo(0);
        assertThat(summary.getTotalReviews()).isEqualTo(0);
        assertThat(summary.getAvgRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("empty: initialises all counters to zero")
    void empty_allZero() {
        ProductRatingSummary summary = ProductRatingSummary.empty(99L, "tenant_001");

        assertThat(summary.getTotalReviews()).isEqualTo(0);
        assertThat(summary.getAvgRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getFiveStar()).isEqualTo(0);
        assertThat(summary.getOneStar()).isEqualTo(0);
    }
}
