package com.hhplus.ecommerce.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 응답")
public record ProductResponse(
        @Schema(description = "상품 ID", example = "1")
        Long productId,

        @Schema(description = "상품명", example = "iPhone 15 Pro")
        String name,

        @Schema(description = "상품 설명", example = "최신 A17 Pro 칩 탑재")
        String description,

        @Schema(description = "가격 (원)", example = "1500000")
        Long price,

        @Schema(description = "카테고리", example = "전자기기")
        String category,

        @Schema(description = "브랜드", example = "Apple")
        String brand,

        @Schema(description = "이미지 URL", example = "https://example.com/images/iphone.jpg")
        String imageUrl,

        @Schema(description = "상품 상태", example = "AVAILABLE", allowableValues = {"AVAILABLE", "OUT_OF_STOCK", "DISCONTINUED"})
        String status,

        @Schema(description = "총 재고 수량", example = "100")
        Integer stock,

        @Schema(description = "예약된 재고 수량", example = "20")
        Integer reservedStock,

        @Schema(description = "구매 가능한 재고 수량", example = "80")
        Integer availableStock,

        @Schema(description = "등록 시간", example = "2025-01-01T00:00:00")
        String createdAt
) {
    public static ProductResponse of(Long productId, String name, String description, Long price,
                                      String category, String brand, String imageUrl, String status,
                                      Integer stock, Integer reservedStock, Integer availableStock,
                                      java.time.LocalDateTime createdAt) {
        return new ProductResponse(
                productId, name, description, price, category, brand, imageUrl, status,
                stock, reservedStock, availableStock,
                createdAt != null ? createdAt.toString() : null
        );
    }
}
