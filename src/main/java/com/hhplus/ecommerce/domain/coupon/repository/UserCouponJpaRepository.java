package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserId(Long userId);

    List<UserCoupon> findByUserIdAndIsUsedFalse(Long userId);

    Optional<UserCoupon> findByCouponIdAndUserId(Long couponId, Long userId);

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId " +
           "AND uc.isUsed = false AND uc.expiresAt > :now")
    List<UserCoupon> findAvailableCouponsByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    List<UserCoupon> findByOrderId(Long orderId);
}
