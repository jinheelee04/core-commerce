package com.hhplus.ecommerce.domain.product.dto;

public record ProductResponse(
        Long productId,
        String name,
        String description,
        Long price,
        String category,
        String brand,
        String imageUrl,
        String status,
        Integer stock,
        Integer reservedStock,
        Integer availableStock,
        String createdAt
) {
}
