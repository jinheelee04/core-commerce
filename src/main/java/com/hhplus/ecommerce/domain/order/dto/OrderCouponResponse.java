package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문에 적용된 쿠폰 정보")
public record OrderCouponResponse(
        @Schema(description = "쿠폰 ID", example = "10")
        Long couponId,

        @Schema(description = "쿠폰명", example = "신규 회원 환영 쿠폰")
        String name,

        @Schema(description = "할인 금액 (원)", example = "10000")
        Long discountAmount
) {
}
