package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장바구니 응답")
public record CartResponse(
        @Schema(description = "장바구니 ID", example = "1")
        Long cartId,

        @Schema(description = "사용자 ID", example = "123")
        Long userId,

        @Schema(description = "장바구니 항목 목록")
        List<CartItemResponse> items,

        @Schema(description = "전체 상품 수량", example = "5")
        Integer totalQuantity,

        @Schema(description = "전체 금액 (원)", example = "150000")
        Long totalAmount,

        @Schema(description = "마지막 업데이트 시간", example = "2025-01-15T10:30:00")
        String updatedAt
) {
}
