package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;

import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 Repository 인터페이스
 * 쿠폰 데이터 접근을 추상화
 */
public interface CouponRepository {
    /**
     * 쿠폰 저장
     */
    Coupon save(Coupon coupon);

    /**
     * ID로 쿠폰 조회
     */
    Optional<Coupon> findById(Long id);

    /**
     * 쿠폰 코드로 조회
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 상태별 쿠폰 조회
     */
    List<Coupon> findByStatus(CouponStatus status);

    /**
     * 발급 가능한 쿠폰 조회
     */
    List<Coupon> findIssuableCoupons();

    /**
     * 모든 쿠폰 조회
     */
    List<Coupon> findAll();

    /**
     * 쿠폰 삭제
     */
    void deleteById(Long id);

    /**
     * 다음 ID 생성
     */
    Long generateNextId();
}
