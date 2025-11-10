package com.hhplus.ecommerce.domain.order.entity;

import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_address_id")
    private Long userAddressId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "items_total", nullable = false)
    private Long itemsTotal = 0L;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "final_amount", nullable = false)
    private Long finalAmount = 0L;

    // 배송지 정보 (스냅샷)
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "delivery_memo", columnDefinition = "TEXT")
    private String deliveryMemo;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
        if (this.itemsTotal == null) {
            this.itemsTotal = 0L;
        }
        if (this.discountAmount == null) {
            this.discountAmount = 0L;
        }
        if (this.finalAmount == null) {
            this.finalAmount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public Order(Long userId, Long userAddressId, String orderNumber,
                 Long itemsTotal, Long discountAmount, Long finalAmount,
                 String recipientName, String recipientPhone, String postalCode,
                 String address, String addressDetail, String deliveryMemo) {
        this.userId = userId;
        this.userAddressId = userAddressId;
        this.orderNumber = orderNumber;
        this.status = OrderStatus.PENDING;
        this.itemsTotal = itemsTotal;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryMemo = deliveryMemo;
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    // 주문 상태 관리
    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문만 결제 처리할 수 있습니다: " + status);
        }
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 확정할 수 있습니다: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        if (!isCancellable()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    // 상태 확인
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.PAID;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
}