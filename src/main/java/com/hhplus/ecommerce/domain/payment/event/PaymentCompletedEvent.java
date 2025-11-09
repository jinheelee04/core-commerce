package com.hhplus.ecommerce.domain.payment.event;

import lombok.Getter;

/**
 * 결제 완료 이벤트
 * 결제가 성공적으로 완료되었을 때 발행되는 도메인 이벤트
 */
@Getter
public class PaymentCompletedEvent {
    private final Long orderId;
    private final Long paymentId;
    private final String transactionId;

    public PaymentCompletedEvent(Long orderId, Long paymentId, String transactionId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.transactionId = transactionId;
    }

    public static PaymentCompletedEvent of(Long orderId, Long paymentId, String transactionId) {
        return new PaymentCompletedEvent(orderId, paymentId, transactionId);
    }
}
