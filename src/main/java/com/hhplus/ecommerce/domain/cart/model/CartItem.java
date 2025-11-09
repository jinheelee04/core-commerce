package com.hhplus.ecommerce.domain.cart.model;

import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CartItem {
    private Long id;
    private Long cartId;
    private Long productId;
    private String productName;
    private Long productPrice;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartItem create(Long id, Long cartId, Long productId, String productName, Long productPrice, int quantity) {
        validateQuantity(quantity);
        LocalDateTime now = LocalDateTime.now();
        return CartItem.builder()
                .id(id)
                .cartId(cartId)
                .productId(productId)
                .productName(productName)
                .productPrice(productPrice)
                .quantity(quantity)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public long getSubtotal() {
        return productPrice * quantity;
    }

    public boolean isSameProduct(Long productId) {
        return this.productId.equals(productId);
    }

    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseQuantity(int amount) {
        int newQuantity = this.quantity + amount;
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
    }
}
