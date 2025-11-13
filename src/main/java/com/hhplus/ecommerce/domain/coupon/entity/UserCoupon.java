package com.hhplus.ecommerce.domain.coupon.entity;

import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_coupons_coupon_user",
            columnNames = {"coupon_id", "user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isUsed == null) {
            this.isUsed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public UserCoupon(Coupon coupon, User user, LocalDateTime expiresAt) {
        this.coupon = coupon;
        this.user = user;
        this.isUsed = false;
        this.expiresAt = expiresAt;
    }

    // 사용 가능 여부 확인
    public boolean isUsable() {
        return !isUsed && !isExpired();
    }

    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // 쿠폰 사용
    public void use(Order order) {
        if (this.isUsed) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        if (isExpired()) {
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }
        this.isUsed = true;
        this.order = order;
        this.usedAt = LocalDateTime.now();
    }

    // 쿠폰 사용 취소 (결제 실패 시)
    public void cancel() {
        if (!this.isUsed) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_USED);
        }
        this.isUsed = false;
        this.order = null;
        this.usedAt = null;
    }

    // 편의 메서드 - 연관관계 ID 조회
    public Long getCouponId() {
        return coupon != null ? coupon.getId() : null;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getOrderId() {
        return order != null ? order.getId() : null;
    }
}
