package com.hhplus.ecommerce.domain.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "쿠폰 발급 요청")
public record IssueCouponRequest(
        @NotNull(message = "쿠폰 ID는 필수입니다")
        @Schema(description = "쿠폰 ID", example = "1", required = true)
        Long couponId
) {
}
