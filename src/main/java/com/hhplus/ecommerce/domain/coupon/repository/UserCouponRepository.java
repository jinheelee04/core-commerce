package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.coupon.id = :couponId AND uc.user.id = :userId")
    Optional<UserCoupon> findByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    @Query("SELECT uc FROM UserCoupon uc " +
           "JOIN FETCH uc.coupon " +
           "JOIN FETCH uc.user " +
           "WHERE uc.user.id = :userId")
    List<UserCoupon> findByUserId(@Param("userId") Long userId);


    @Query("SELECT uc FROM UserCoupon uc " +
           "JOIN FETCH uc.coupon " +
           "JOIN FETCH uc.user " +
           "WHERE uc.user.id = :userId AND uc.isUsed = :isUsed")
    List<UserCoupon> findByUserIdAndIsUsed(@Param("userId") Long userId, @Param("isUsed") Boolean isUsed);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.order.id = :orderId")
    List<UserCoupon> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END FROM UserCoupon uc WHERE uc.coupon.id = :couponId AND uc.user.id = :userId")
    boolean existsByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") Long userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.id = :userId " +
           "AND uc.isUsed = false AND uc.expiresAt > :now")
    List<UserCoupon> findAvailableCouponsByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.id = :couponId")
    long countByCouponId(@Param("couponId") Long couponId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user.id = :userId AND uc.coupon.id = :couponId")
    long countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
