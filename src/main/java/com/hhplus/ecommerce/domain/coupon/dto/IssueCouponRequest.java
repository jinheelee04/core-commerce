package com.hhplus.ecommerce.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest(
        @Schema(description = "쿠폰 ID", example = "1", required = true)
        Long couponId
) {
}
