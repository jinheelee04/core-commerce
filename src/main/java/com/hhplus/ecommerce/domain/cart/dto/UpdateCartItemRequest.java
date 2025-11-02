package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 수량 변경 요청")
public record UpdateCartItemRequest(
        @Schema(description = "변경할 수량", example = "3", required = true, minimum = "1")
        Integer quantity
) {
}
