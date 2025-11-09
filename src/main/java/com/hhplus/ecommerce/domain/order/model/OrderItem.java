package com.hhplus.ecommerce.domain.order.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Long unitPrice;
    private Long subtotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long calculateSubtotal() {
        return unitPrice * quantity;
    }

    public static OrderItem create(Long id, Long productId, String productName, Integer quantity, Long unitPrice) {
        LocalDateTime now = LocalDateTime.now();
        long subtotal = unitPrice * quantity;

        return OrderItem.builder()
                .id(id)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public OrderItem withOrderId(Long orderId) {
        return OrderItem.builder()
                .id(this.id)
                .orderId(orderId)
                .productId(this.productId)
                .productName(this.productName)
                .quantity(this.quantity)
                .unitPrice(this.unitPrice)
                .subtotal(this.subtotal)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
