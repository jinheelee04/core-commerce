package com.hhplus.ecommerce.domain.category.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직용 생성자
    public Category(Long parentId, String name, String nameEn, Integer level,
                    Integer displayOrder, String imageUrl) {
        this.parentId = parentId;
        this.name = name;
        this.nameEn = nameEn;
        this.level = level;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.imageUrl = imageUrl;
        this.isActive = true;
    }

    // 카테고리 정보 수정
    public void updateInfo(String name, String nameEn, Integer displayOrder, String imageUrl) {
        this.name = name;
        this.nameEn = nameEn;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        this.imageUrl = imageUrl;
    }

    // 활성화/비활성화
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // 최상위 카테고리 여부 확인
    public boolean isTopLevel() {
        return this.parentId == null;
    }
}
