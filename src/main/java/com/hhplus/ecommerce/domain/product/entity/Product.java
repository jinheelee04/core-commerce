package com.hhplus.ecommerce.domain.product.entity;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 통계 필드 (집계용)
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "sales_count")
    private Integer salesCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProductStatus.ACTIVE;
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.salesCount == null) {
            this.salesCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 생성자 ==========

    public Product(Category category, Brand brand, String name, String description,
                   Long price, String imageUrl) {
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.ACTIVE;
    }

    // ========== 비즈니스 로직 ==========

    // 상품 정보 수정
    public void updateInfo(String name, String description, Long price, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    // 상태 변경
    public void updateStatus(ProductStatus newStatus) {
        this.status = newStatus;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    // 상태 확인
    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }

    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE;
    }

    // 통계 관리
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public void incrementSalesCount(int quantity) {
        this.salesCount = (this.salesCount == null ? 0 : this.salesCount) + quantity;
    }

    public int getPopularityScore() {
        int views = this.viewCount == null ? 0 : this.viewCount;
        int sales = this.salesCount == null ? 0 : this.salesCount;
        return views + (sales * 10);
    }
}