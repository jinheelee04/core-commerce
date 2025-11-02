package com.hhplus.ecommerce.domain.cart.dto;

import java.util.List;

public record CartResponse(
        Long cartId,
        Long userId,
        List<CartItemResponse> items,
        Integer totalQuantity,
        Long totalAmount,
        String updatedAt
) {
}
