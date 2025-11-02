package com.hhplus.ecommerce.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 응답")
public record CouponResponse(
        @Schema(description = "쿠폰 ID", example = "1")
        Long couponId,

        @Schema(description = "쿠폰 코드", example = "WELCOME2025")
        String code,

        @Schema(description = "쿠폰명", example = "신규 회원 환영 쿠폰")
        String name,

        @Schema(description = "쿠폰 설명", example = "첫 구매 시 10% 할인")
        String description,

        @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = {"PERCENTAGE", "FIXED_AMOUNT"})
        String discountType,

        @Schema(description = "할인 값 (PERCENTAGE: %, FIXED_AMOUNT: 원)", example = "10")
        Integer discountValue,

        @Schema(description = "최소 주문 금액 (원)", example = "50000")
        Long minOrderAmount,

        @Schema(description = "최대 할인 금액 (원)", example = "10000")
        Long maxDiscountAmount,

        @Schema(description = "총 발급 수량", example = "1000")
        Integer totalQuantity,

        @Schema(description = "남은 수량", example = "850")
        Integer remainingQuantity,

        @Schema(description = "발급 시작 시간", example = "2025-01-01T00:00:00")
        String startsAt,

        @Schema(description = "발급 종료 시간", example = "2025-12-31T23:59:59")
        String endsAt,

        @Schema(description = "쿠폰 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "EXPIRED"})
        String status
) {
}
