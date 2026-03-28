package com.iemodo.marketing.repository;

import com.iemodo.marketing.domain.UserCoupon;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User coupon repository
 */
@Repository
public interface UserCouponRepository extends ReactiveCrudRepository<UserCoupon, Long> {

    Mono<UserCoupon> findByCustomerIdAndCouponIdAndIsValid(Long customerId, Long couponId);

    Flux<UserCoupon> findByCustomerIdAndTenantIdAndIsValid(Long customerId, String tenantId);

    Flux<UserCoupon> findByCouponIdAndCouponStatus(Long couponId, UserCoupon.UserCouponStatus couponStatus);
}
