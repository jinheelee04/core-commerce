package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 항목 응답")
public record CartItemResponse(
        @Schema(description = "장바구니 항목 ID", example = "1")
        Long cartItemId,

        @Schema(description = "상품 ID", example = "10")
        Long productId,

        @Schema(description = "상품명", example = "iPhone 15 Pro")
        String productName,

        @Schema(description = "상품 이미지 URL", example = "https://example.com/images/iphone.jpg")
        String productImageUrl,

        @Schema(description = "상품 가격 (원)", example = "1500000")
        Long price,

        @Schema(description = "수량", example = "2")
        Integer quantity,

        @Schema(description = "소계 (원)", example = "3000000")
        Long subtotal,

        @Schema(description = "현재 재고 수량", example = "50")
        Integer availableStock,

        @Schema(description = "생성 시간", example = "2025-01-15T10:30:00")
        String createdAt
) {
}
