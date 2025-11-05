package com.hhplus.ecommerce.domain.payment.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Payment {
    private Long id;
    private Long orderId;
    private Long amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String clientRequestId;
    private String transactionId;
    private String failReason;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markAsSuccess(String transactionId) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("성공한 결제만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public static Payment createPending(Long id, Long orderId, Long amount, PaymentMethod paymentMethod, String clientRequestId) {
        LocalDateTime now = LocalDateTime.now();
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .clientRequestId(clientRequestId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
