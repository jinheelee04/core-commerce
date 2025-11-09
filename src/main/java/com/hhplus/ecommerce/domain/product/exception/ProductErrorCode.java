package com.hhplus.ecommerce.domain.product.exception;

import com.hhplus.ecommerce.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_OUT_OF_STOCK", "품절된 상품입니다"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "재고가 부족합니다"),
    INSUFFICIENT_RESERVED_STOCK(HttpStatus.BAD_REQUEST, "INSUFFICIENT_RESERVED_STOCK", "예약 재고가 부족합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
