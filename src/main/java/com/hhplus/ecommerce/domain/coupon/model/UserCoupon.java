package com.hhplus.ecommerce.domain.coupon.model;

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
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        if (!isUsable()) {
            throw new IllegalStateException("사용 불가능한 쿠폰입니다.");
        }
        this.isUsed = true;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancelUse() {
        if (!this.isUsed) {
            throw new IllegalStateException("사용되지 않은 쿠폰입니다.");
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
