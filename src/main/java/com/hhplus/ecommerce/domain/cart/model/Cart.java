package com.hhplus.ecommerce.domain.cart.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Cart {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Cart create(Long id, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return Cart.builder()
                .id(id)
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
