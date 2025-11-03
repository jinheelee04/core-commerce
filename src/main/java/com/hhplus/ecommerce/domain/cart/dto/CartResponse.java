package com.hhplus.ecommerce.domain.cart.dto;

import com.hhplus.ecommerce.domain.cart.model.Cart;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "장바구니 응답")
public record CartResponse(
        @Schema(description = "장바구니 ID", example = "1")
        Long cartId,

        @Schema(description = "사용자 ID", example = "123")
        Long userId,

        @Schema(description = "장바구니 항목 목록")
        List<CartItemResponse> items,

        @Schema(description = "전체 상품 수량", example = "5")
        Integer totalQuantity,

        @Schema(description = "전체 금액 (원)", example = "150000")
        Long totalAmount,

        @Schema(description = "마지막 업데이트 시간", example = "2025-01-15T10:30:00")
        String updatedAt
) {

    public static CartResponse from(Cart cart, List<CartItemResponse> items) {
        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        long totalAmount = items.stream()
                .mapToLong(CartItemResponse::subtotal)
                .sum();

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                items,
                totalQuantity,
                totalAmount,
                cart.getUpdatedAt() != null ? cart.getUpdatedAt().toString() : null
        );
    }

    public static CartResponse empty(Long userId) {
        return new CartResponse(null, userId, List.of(), 0, 0L, null);
    }

    public static CartResponse of(Long cartId, Long userId, List<CartItemResponse> items,
                                   Integer totalQuantity, Long totalAmount,
                                   java.time.LocalDateTime updatedAt) {
        return new CartResponse(
                cartId, userId, items, totalQuantity, totalAmount,
                updatedAt != null ? updatedAt.toString() : null
        );
    }
}
