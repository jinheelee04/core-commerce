package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 항목 정보")
public record OrderItemResponse(
        @Schema(description = "주문 항목 ID", example = "1")
        Long orderItemId,

        @Schema(description = "상품 ID", example = "10")
        Long productId,

        @Schema(description = "상품명", example = "iPhone 15 Pro")
        String productName,

        @Schema(description = "수량", example = "2")
        Integer quantity,

        @Schema(description = "상품 가격 (원)", example = "1500000")
        Long price,

        @Schema(description = "소계 (원)", example = "3000000")
        Long subtotal
) {
}
