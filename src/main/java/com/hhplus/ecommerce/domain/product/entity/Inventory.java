package com.hhplus.ecommerce.domain.product.entity;

import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.stock == null) {
            this.stock = 0;
        }
        if (this.reservedStock == null) {
            this.reservedStock = 0;
        }
        if (this.lowStockThreshold == null) {
            this.lowStockThreshold = 10;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 생성자 ==========

    public Inventory(Product product, Integer stock, Integer lowStockThreshold) {
        this.product = product;
        this.stock = stock != null ? stock : 0;
        this.reservedStock = 0;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 10;
    }

    // ========== 비즈니스 로직 ==========

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
    }

    public void releaseReservation(int quantity) {
        if (this.reservedStock < quantity) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_RESERVED_STOCK);
        }
        this.reservedStock -= quantity;
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
    }

    public void addStock(int quantity) {
        this.stock += quantity;
    }

    public void updateStock(int newStock) {
        if (newStock < this.reservedStock) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock = newStock;
    }
}
