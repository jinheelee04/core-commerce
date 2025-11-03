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
}
