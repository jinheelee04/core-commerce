package com.hhplus.ecommerce.domain.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장바구니 수량 변경 요청")
public record UpdateCartItemRequest(
        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다")
        @Schema(description = "변경할 수량", example = "3", required = true, minimum = "1")
        Integer quantity
) {
}
