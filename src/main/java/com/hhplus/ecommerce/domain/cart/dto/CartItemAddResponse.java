package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 항목 추가/수정 응답")
public record CartItemAddResponse(
        @Schema(description = "장바구니 항목 ID", example = "1")
        Long cartItemId,

        @Schema(description = "상품 ID", example = "10")
        Long productId,

        @Schema(description = "상품명", example = "iPhone 15 Pro")
        String productName,

        @Schema(description = "수량", example = "2")
        Integer quantity,

        @Schema(description = "소계 (원)", example = "3000000")
        Long subtotal,

        @Schema(description = "생성/수정 시간", example = "2025-01-15T10:30:00")
        String createdAt
) {
    public static CartItemAddResponse of(Long cartItemId, Long productId, String productName,
                                          Integer quantity, Long subtotal,
                                          java.time.LocalDateTime createdAt) {
        return new CartItemAddResponse(
                cartItemId, productId, productName, quantity, subtotal,
                createdAt != null ? createdAt.toString() : null
        );
    }
}
