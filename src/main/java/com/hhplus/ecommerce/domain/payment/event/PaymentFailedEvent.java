package com.hhplus.ecommerce.domain.payment.event;

import lombok.Getter;

/**
 * 결제 실패 이벤트
 * 결제가 실패했을 때 발행되는 도메인 이벤트
 */
@Getter
public class PaymentFailedEvent {
    private final Long orderId;
    private final Long paymentId;
    private final String failReason;

    public PaymentFailedEvent(Long orderId, Long paymentId, String failReason) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.failReason = failReason;
    }

    public static PaymentFailedEvent of(Long orderId, Long paymentId, String failReason) {
        return new PaymentFailedEvent(orderId, paymentId, failReason);
    }
}
