package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 상품 추가 요청")
public record AddCartItemRequest(
        @Schema(description = "상품 ID", example = "1", required = true)
        Long productId,

        @Schema(description = "수량", example = "2", required = true, minimum = "1")
        Integer quantity
) {
}
