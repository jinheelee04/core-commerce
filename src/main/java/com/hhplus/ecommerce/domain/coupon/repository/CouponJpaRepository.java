package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithLock(@Param("code") String code);

    List<Coupon> findByStatus(CouponStatus status);

    @Query("SELECT c FROM Coupon c WHERE c.status = :status " +
           "AND c.startsAt <= :now AND c.endsAt > :now")
    List<Coupon> findActiveAndAvailableCoupons(
            @Param("status") CouponStatus status,
            @Param("now") LocalDateTime now
    );

    boolean existsByCode(String code);
}
