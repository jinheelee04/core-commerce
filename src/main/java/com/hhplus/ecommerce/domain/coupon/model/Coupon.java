package com.hhplus.ecommerce.domain.coupon.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Coupon {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private Integer discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private CouponStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isIssuable() {
        return status == CouponStatus.ACTIVE
                && remainingQuantity > 0
                && LocalDateTime.now().isBefore(endsAt);
    }

    public boolean isUsable() {
        LocalDateTime now = LocalDateTime.now();
        return status == CouponStatus.ACTIVE
                && now.isAfter(startsAt)
                && now.isBefore(endsAt);
    }

    public void issue() {
        if (remainingQuantity <= 0) {
            throw new IllegalStateException("쿠폰 발급 수량이 소진되었습니다.");
        }
        if (!isIssuable()) {
            throw new IllegalStateException("발급 불가능한 쿠폰입니다.");
        }
        this.remainingQuantity--;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancelIssue() {
        if (this.remainingQuantity >= this.totalQuantity) {
            throw new IllegalStateException("발급 취소할 수 없습니다.");
        }
        this.remainingQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    public long calculateDiscount(long orderAmount) {
        if (!isUsable()) {
            throw new IllegalStateException("사용 불가능한 쿠폰입니다.");
        }
        if (orderAmount < minOrderAmount) {
            throw new IllegalStateException("최소 주문 금액을 충족하지 않습니다.");
        }

        long discount = 0;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount * discountValue / 100;
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            discount = discountValue;
        }

        return discount;
    }

    public static Coupon create(Long id, String code, String name, String description,
                                DiscountType discountType, Integer discountValue,
                                Long minOrderAmount, Long maxDiscountAmount,
                                Integer totalQuantity, LocalDateTime startsAt, LocalDateTime endsAt) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder()
                .id(id)
                .code(code)
                .name(name)
                .description(description)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .totalQuantity(totalQuantity)
                .remainingQuantity(totalQuantity)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}