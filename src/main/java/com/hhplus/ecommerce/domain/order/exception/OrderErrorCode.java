package com.hhplus.ecommerce.domain.order.exception;

import com.hhplus.ecommerce.global.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다"),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "ORDER_ALREADY_CANCELLED", "이미 취소된 주문입니다"),
    ORDER_ALREADY_PAID(HttpStatus.BAD_REQUEST, "ORDER_ALREADY_PAID", "이미 결제된 주문은 취소할 수 없습니다"),
    ORDER_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "ORDER_ALREADY_CONFIRMED", "이미 확정된 주문은 취소할 수 없습니다"),
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_ORDER_REQUEST", "유효하지 않은 주문 요청입니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "지원하지 않는 작업입니다"),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED", "본인의 주문만 접근할 수 있습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
