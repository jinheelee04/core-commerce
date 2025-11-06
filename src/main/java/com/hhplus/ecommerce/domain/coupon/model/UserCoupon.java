package com.hhplus.ecommerce.domain.coupon.model;

import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserCoupon {
    private Long id;
    private Long couponId;
    private Long userId;
    private Long orderId;
    private Boolean isUsed;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;

    public boolean isUsable() {
        return !isUsed && LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void use(Long orderId) {
        if (this.isUsed) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }
        this.isUsed = true;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancelUse() {
        if (!this.isUsed) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_USED);
        }
        this.isUsed = false;
        this.orderId = null;
        this.usedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public static UserCoupon issue(Long id, Long couponId, Long userId, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return UserCoupon.builder()
                .id(id)
                .couponId(couponId)
                .userId(userId)
                .isUsed(false)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .updatedAt(now)
                .build();
    }
}
