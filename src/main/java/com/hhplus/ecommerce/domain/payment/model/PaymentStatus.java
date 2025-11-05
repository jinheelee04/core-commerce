package com.hhplus.ecommerce.domain.payment.model;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    PENDING,   // 결제 대기
    SUCCESS,   // 결제 성공
    FAILED,    // 결제 실패
    CANCELLED  // 결제 취소
}