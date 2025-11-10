package com.hhplus.ecommerce.domain.coupon.exception;

import com.hhplus.ecommerce.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponErrorCode implements ErrorCode {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "쿠폰을 찾을 수 없습니다"),
    COUPON_INACTIVE(HttpStatus.BAD_REQUEST, "COUPON_INACTIVE", "발급할 수 없는 쿠폰입니다"),
    COUPON_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "COUPON_OUT_OF_STOCK", "쿠폰이 모두 소진되었습니다"),
    COUPON_ALREADY_ISSUED(HttpStatus.BAD_REQUEST, "COUPON_ALREADY_ISSUED", "이미 발급받은 쿠폰입니다"),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "COUPON_ALREADY_USED", "이미 사용된 쿠폰입니다"),
    COUPON_NOT_STARTED(HttpStatus.BAD_REQUEST, "COUPON_NOT_STARTED", "아직 발급이 시작되지 않은 쿠폰입니다"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_EXPIRED", "쿠폰 발급 기간이 만료되었습니다"),
    COUPON_NOT_ISSUABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_ISSUABLE", "발급 불가능한 쿠폰입니다"),
    COUPON_CANNOT_CANCEL_ISSUE(HttpStatus.BAD_REQUEST, "COUPON_CANNOT_CANCEL_ISSUE", "발급 취소할 수 없습니다"),
    COUPON_NOT_USABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_USABLE", "사용 불가능한 쿠폰입니다"),
    COUPON_NOT_USED(HttpStatus.BAD_REQUEST, "COUPON_NOT_USED", "사용되지 않은 쿠폰입니다"),
    COUPON_ALREADY_RESERVED(HttpStatus.BAD_REQUEST, "COUPON_ALREADY_RESERVED", "이미 다른 주문에 예약된 쿠폰입니다"),
    COUPON_NOT_RESERVED(HttpStatus.BAD_REQUEST, "COUPON_NOT_RESERVED", "예약되지 않은 쿠폰입니다"),
    COUPON_RESTORE_FAILED(HttpStatus.BAD_REQUEST, "COUPON_RESTORE_FAILED", "쿠폰 복구에 실패했습니다"),
    MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "MIN_ORDER_AMOUNT_NOT_MET", "쿠폰 사용을 위한 최소 주문 금액을 충족하지 못했습니다"),
    COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST,"COUPON_MIN_ORDER_AMOUNT_NOT_MET", "쿠폰 사용을 위한 최소 주문 금액을 충족하지 못했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
