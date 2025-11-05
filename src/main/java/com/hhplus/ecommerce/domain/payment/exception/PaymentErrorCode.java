package com.hhplus.ecommerce.domain.payment.exception;

import com.hhplus.ecommerce.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", "결제 대기 상태의 주문만 결제할 수 있습니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_FAILED", "결제 처리 중 오류가 발생했습니다"),
    PAYMENT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "PAYMENT_NOT_ALLOWED", "해당 결제에 접근할 권한이 없습니다."),
    PG_COMMUNICATION_FAILED(HttpStatus.FORBIDDEN, "PG_COMMUNICATION_FAILED", "결제 서버와의 통신에 실패했습니다." );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
