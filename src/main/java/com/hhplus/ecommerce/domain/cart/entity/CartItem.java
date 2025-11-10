package com.hhplus.ecommerce.domain.cart.entity;

import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cart_items_cart_product",
            columnNames = {"cart_id", "product_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null) {
            this.quantity = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public CartItem(Long cartId, Long productId, Integer quantity) {
        validateQuantity(quantity);
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // 수량 검증
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(CartErrorCode.INVALID_QUANTITY);
        }
    }

    // 상품 확인
    public boolean isSameProduct(Long productId) {
        return this.productId.equals(productId);
    }

    // 수량 변경
    public void updateQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    // 수량 증가
    public void increaseQuantity(int amount) {
        int newQuantity = this.quantity + amount;
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }
}