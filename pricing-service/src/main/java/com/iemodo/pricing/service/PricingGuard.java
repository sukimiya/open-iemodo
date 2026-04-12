package com.iemodo.pricing.service;

import com.iemodo.pricing.dto.PriceDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validates computed {@link PriceDetail} results for anomalies before they are
 * returned to callers.
 *
 * <p>Anomalies are logged as structured warnings (tag: {@code [PRICE_ANOMALY]})
 * so alerting rules can fire on them. The price is never silently corrected —
 * the caller receives the original result along with the warning so that the
 * data pipeline retains full fidelity.
 *
 * <p>Current checks:
 * <ul>
 *   <li><b>Non-positive final price</b> — finalPrice &le; 0</li>
 *   <li><b>Discount exceeds base price</b> — discounts &gt; basePrice</li>
 *   <li><b>Excessive discount rate</b> — discounts / basePrice &gt; 90%</li>
 *   <li><b>Negative discount</b> — discounts &lt; 0 (price was inflated, not reduced)</li>
 * </ul>
 */
@Slf4j
@Component
public class PricingGuard {

    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.90");

    /**
     * Validates {@code detail} and logs a warning for every anomaly found.
     * Always returns the original {@code detail} unchanged.
     */
    public PriceDetail validate(PriceDetail detail, String sku) {
        if (detail == null) return null;

        BigDecimal finalPrice = detail.getFinalPrice();
        BigDecimal basePrice  = detail.getBasePrice();
        BigDecimal discounts  = detail.getDiscounts();

        if (finalPrice != null && finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[PRICE_ANOMALY] sku={} finalPrice={} is non-positive", sku, finalPrice);
        }

        if (discounts != null && basePrice != null) {
            if (discounts.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[PRICE_ANOMALY] sku={} discounts={} is negative (price inflated)", sku, discounts);
            } else if (discounts.compareTo(basePrice) > 0) {
                log.warn("[PRICE_ANOMALY] sku={} discounts={} exceeds basePrice={}", sku, discounts, basePrice);
            } else if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = discounts.divide(basePrice, 4, java.math.RoundingMode.HALF_UP);
                if (rate.compareTo(MAX_DISCOUNT_RATE) > 0) {
                    log.warn("[PRICE_ANOMALY] sku={} discount rate={}% exceeds 90% threshold",
                            sku, rate.multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP));
                }
            }
        }

        return detail;
    }
}
