package com.iemodo.marketing.repository;

import com.iemodo.marketing.domain.Coupon;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Coupon repository
 */
@Repository
public interface CouponRepository extends ReactiveCrudRepository<Coupon, Long> {

    Mono<Coupon> findByCouponCodeAndTenantId(String couponCode, String tenantId);

    Mono<Coupon> findByCouponCodeAndTenantIdAndIsValid(String couponCode, String tenantId);

    Flux<Coupon> findByTenantIdAndIsValid(String tenantId);

    @Query("SELECT * FROM coupons WHERE tenant_id = :tenantId " +
           "AND is_active = true AND is_valid = 1 " +
           "AND valid_from <= :now AND valid_to >= :now " +
           "ORDER BY create_time DESC")
    Flux<Coupon> findActiveCoupons(String tenantId, Instant now);

    @Query("SELECT * FROM coupons WHERE tenant_id = :tenantId " +
           "AND type = :type AND is_active = true AND is_valid = 1 " +
           "ORDER BY create_time DESC")
    Flux<Coupon> findByType(String tenantId, String type);
}
