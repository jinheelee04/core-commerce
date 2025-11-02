package com.hhplus.ecommerce.domain.cart.dto;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        String productImageUrl,
        Long price,
        Integer quantity,
        Long subtotal,
        Integer availableStock,
        String createdAt
) {
}
