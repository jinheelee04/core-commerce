package com.hhplus.ecommerce.domain.product.model;

import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Inventory {
    private Long id;
    private Long productId;
    private Integer stock;
    private Integer reservedStock;
    private Integer lowStockThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Inventory empty() {
        return Inventory.builder()
                .productId(null)
                .stock(0)
                .reservedStock(0)
                .build();
    }

    public int getAvailableStock() {
        return stock - reservedStock;
    }

    public boolean isLowStock() {
        return getAvailableStock() <= lowStockThreshold;
    }

    public void reserve(int quantity) {
        if (getAvailableStock() < quantity) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.reservedStock += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseReservation(int quantity) {
        if (this.reservedStock < quantity) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);
        }
        this.reservedStock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmReservation(int quantity) {
        if (this.reservedStock < quantity) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);
        }
        if (this.stock < quantity) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
        this.reservedStock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void addStock(int quantity) {
        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
