package com.iemodo.marketing.service;

import com.iemodo.marketing.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Auto-publishes draft coupons when their {@code valid_from} time is reached.
 *
 * <p>Coupons created with {@code isActive=false} go live automatically once
 * {@code valid_from <= now}, removing the need for manual publication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponPublishScheduler {

    private final CouponRepository couponRepository;

    @Scheduled(fixedDelay = 60_000)
    public void publishScheduledCoupons() {
        couponRepository.findScheduledToActivate(Instant.now())
                .flatMap(coupon -> {
                    coupon.setIsActive(true);
                    return couponRepository.save(coupon);
                })
                .subscribe(
                        coupon -> log.info("Auto-published coupon: {} ({})", coupon.getId(), coupon.getCouponCode()),
                        ex -> log.error("Error auto-publishing coupons", ex)
                );
    }
}
