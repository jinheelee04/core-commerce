package com.hhplus.ecommerce.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 보유 쿠폰 응답")
public record UserCouponResponse(
        @Schema(description = "사용자 쿠폰 ID", example = "1")
        Long userCouponId,

        @Schema(description = "쿠폰 ID", example = "10")
        Long couponId,

        @Schema(description = "사용자 ID", example = "123")
        Long userId,

        @Schema(description = "쿠폰 코드", example = "WELCOME2025")
        String code,

        @Schema(description = "쿠폰명", example = "신규 회원 환영 쿠폰")
        String name,

        @Schema(description = "할인 타입", example = "PERCENTAGE", allowableValues = {"PERCENTAGE", "FIXED_AMOUNT"})
        String discountType,

        @Schema(description = "할인 값 (PERCENTAGE: %, FIXED_AMOUNT: 원)", example = "10")
        Integer discountValue,

        @Schema(description = "최소 주문 금액 (원)", example = "50000")
        Long minOrderAmount,

        @Schema(description = "최대 할인 금액 (원)", example = "10000")
        Long maxDiscountAmount,

        @Schema(description = "사용 여부", example = "false")
        Boolean isUsed,

        @Schema(description = "발급 시간", example = "2025-01-15T10:30:00")
        String issuedAt,

        @Schema(description = "사용 시간", example = "2025-01-20T15:45:00")
        String usedAt,

        @Schema(description = "만료 시간", example = "2025-02-14T10:30:00")
        String expiresAt
) {
}
