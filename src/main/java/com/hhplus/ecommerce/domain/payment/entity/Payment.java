package com.hhplus.ecommerce.domain.payment.entity;

import com.hhplus.ecommerce.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "payment_method", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "client_request_id", unique = true, length = 100)
    private String clientRequestId;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public Payment(Order order, Long amount, PaymentMethod paymentMethod, String clientRequestId) {
        this.order = order;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.clientRequestId = clientRequestId;
        this.status = PaymentStatus.PENDING;
    }

    // 결제 성공 처리
    public void markAsSuccess(String transactionId) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    // 결제 실패 처리
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        this.failedAt = LocalDateTime.now();
    }

    // 결제 취소
    public void cancel() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("성공한 결제만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    // 상태 확인
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    // 편의 메서드
    public Long getOrderId() {
        return order != null ? order.getId() : null;
    }

    public enum PaymentMethod {
        CARD,           // 카드
        VIRTUAL_ACCOUNT, // 가상계좌
        PHONE           // 휴대폰
    }

    /**
     * 결제 상태
     */
    public enum PaymentStatus {
        PENDING,   // 결제 대기
        SUCCESS,   // 결제 성공
        FAILED,    // 결제 실패
        CANCELLED  // 결제 취소
    }
}