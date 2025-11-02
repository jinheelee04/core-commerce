package com.hhplus.ecommerce.domain.product.model.product;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Product {
    private Long id;
    private String name;
    private String description;
    private Long price;
    private ProductCategory category;
    private String brand;
    private String imageUrl;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return status == ProductStatus.AVAILABLE;
    }

    public void updateStatus(ProductStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String description, Long price, ProductCategory category, String brand, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
