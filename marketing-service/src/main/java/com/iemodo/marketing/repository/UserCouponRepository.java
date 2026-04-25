package com.iemodo.marketing.repository;

import com.iemodo.marketing.domain.UserCoupon;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User coupon repository
 */
@Repository
public interface UserCouponRepository extends ReactiveCrudRepository<UserCoupon, Long> {

    @Query("SELECT * FROM user_coupons WHERE customer_id = :customerId AND coupon_id = :couponId AND is_valid = true")
    Mono<UserCoupon> findByCustomerIdAndCouponIdAndIsValid(Long customerId, Long couponId);

    @Query("SELECT * FROM user_coupons WHERE customer_id = :customerId AND tenant_id = :tenantId AND is_valid = true")
    Flux<UserCoupon> findByCustomerIdAndTenantIdAndIsValid(Long customerId, String tenantId);

    Flux<UserCoupon> findByCouponIdAndCouponStatus(Long couponId, UserCoupon.UserCouponStatus couponStatus);
}
