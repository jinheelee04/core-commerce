package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 쿠폰 Repository 인터페이스
 * 사용자 쿠폰 데이터 접근을 추상화
 */
public interface UserCouponRepository {
    /**
     * 사용자 쿠폰 저장
     */
    UserCoupon save(UserCoupon userCoupon);

    /**
     * ID로 사용자 쿠폰 조회
     */
    Optional<UserCoupon> findById(Long id);

    /**
     * 쿠폰 ID와 사용자 ID로 조회
     */
    Optional<UserCoupon> findByCouponIdAndUserId(Long couponId, Long userId);

    /**
     * 사용자 ID로 쿠폰 목록 조회
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 사용자 ID와 사용 여부로 쿠폰 조회
     */
    List<UserCoupon> findByUserIdAndIsUsed(Long userId, Boolean isUsed);

    /**
     * 주문 ID로 사용된 쿠폰 조회
     */
    Optional<UserCoupon> findByOrderId(Long orderId);

    /**
     * 모든 사용자 쿠폰 조회
     */
    List<UserCoupon> findAll();

    /**
     * 사용자 쿠폰 삭제
     */
    void deleteById(Long id);

    /**
     * 다음 ID 생성
     */
    Long generateNextId();
}
