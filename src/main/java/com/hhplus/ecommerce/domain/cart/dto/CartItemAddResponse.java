package com.hhplus.ecommerce.domain.cart.dto;

public record CartItemAddResponse(
        Long cartItemId,
        Long productId,
        String productName,
        Integer quantity,
        Long subtotal,
        String createdAt
) {
}
