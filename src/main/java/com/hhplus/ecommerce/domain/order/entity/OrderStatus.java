package com.hhplus.ecommerce.domain.order.entity;

/**
 * 주문 상태
 */
public enum OrderStatus {
    PENDING,    // 주문 생성 (결제 대기)
    PAID,       // 결제 완료
    CONFIRMED,  // 주문 확정
    CANCELLED   // 주문 취소
}
