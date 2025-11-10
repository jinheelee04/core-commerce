package com.hhplus.ecommerce.domain.coupon.entity;

import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "min_order_amount", nullable = false)
    private Long minOrderAmount = 0L;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CouponStatus.ACTIVE;
        }
        if (this.minOrderAmount == null) {
            this.minOrderAmount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public Coupon(String code, String name, String description, DiscountType discountType,
                  Integer discountValue, Long minOrderAmount, Long maxDiscountAmount,
                  Integer totalQuantity, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0L;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = CouponStatus.ACTIVE;
    }

    // 발급 가능 여부 확인
    public boolean isIssuable() {
        return status == CouponStatus.ACTIVE
                && remainingQuantity > 0
                && LocalDateTime.now().isBefore(endsAt);
    }

    // 사용 가능 여부 확인
    public boolean isUsable() {
        LocalDateTime now = LocalDateTime.now();
        return status == CouponStatus.ACTIVE
                && now.isAfter(startsAt)
                && now.isBefore(endsAt);
    }

    // 쿠폰 발급 (재고 차감)
    public void issue() {
        if (remainingQuantity <= 0) {
            throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
        }
        if (status != CouponStatus.ACTIVE) {
            throw new BusinessException(CouponErrorCode.COUPON_INACTIVE);
        }
        if (!LocalDateTime.now().isBefore(endsAt)) {
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }
        this.remainingQuantity--;
    }

    // 쿠폰 사용 취소 (재고 복구)
    public void restore() {
        if (this.remainingQuantity >= this.totalQuantity) {
            throw new BusinessException(CouponErrorCode.COUPON_RESTORE_FAILED);
        }
        this.remainingQuantity++;
    }

    // 할인 금액 계산
    public long calculateDiscountAmount(long orderAmount) {
        if (orderAmount < minOrderAmount) {
            throw new BusinessException(CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET);
        }

        long discountAmount = switch (discountType) {
            case PERCENTAGE -> orderAmount * discountValue / 100;
            case FIXED_AMOUNT -> discountValue;
        };

        // 최대 할인 금액 제한
        if (maxDiscountAmount != null && discountAmount > maxDiscountAmount) {
            discountAmount = maxDiscountAmount;
        }

        return discountAmount;
    }

    // 상태 변경
    public void activate() {
        this.status = CouponStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CouponStatus.INACTIVE;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }
}