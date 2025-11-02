package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 가격 정보")
public record OrderPricingResponse(
        @Schema(description = "상품 총액 (원)", example = "100000")
        Long itemsTotal,

        @Schema(description = "할인 금액 (원)", example = "10000")
        Long discountAmount,

        @Schema(description = "최종 결제 금액 (원)", example = "90000")
        Long finalAmount
) {
}
