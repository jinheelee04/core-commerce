package com.hhplus.ecommerce.global.common.enums;

public enum ErrorCode {
    PRODUCT_NOT_FOUND("상품을 찾을 수 없습니다"),
    PRODUCT_OUT_OF_STOCK("품절된 상품입니다"),
    EMPTY_CART("장바구니가 비어있습니다"),
    CART_ITEM_NOT_FOUND("장바구니 항목을 찾을 수 없습니다"),
    COUPON_NOT_FOUND("쿠폰을 찾을 수 없습니다"),
    COUPON_ALREADY_ISSUED("이미 발급받은 쿠폰입니다"),
    COUPON_OUT_OF_STOCK("쿠폰이 모두 소진되었습니다"),
    COUPON_EXPIRED("만료된 쿠폰입니다"),
    COUPON_NOT_STARTED("아직 사용할 수 없는 쿠폰입니다"),
    COUPON_ALREADY_USED("이미 사용된 쿠폰입니다"),
    MIN_ORDER_AMOUNT_NOT_MET("최소 주문 금액을 충족하지 않습니다"),
    ORDER_NOT_FOUND("주문을 찾을 수 없습니다"),
    ORDER_ALREADY_PAID("이미 결제가 완료된 주문입니다"),
    PAYMENT_AMOUNT_MISMATCH("결제 금액이 주문 금액과 일치하지 않습니다"),
    PAYMENT_FAILED("결제에 실패했습니다"),
    INSUFFICIENT_STOCK("재고가 부족합니다");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
