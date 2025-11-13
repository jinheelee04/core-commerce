package com.hhplus.ecommerce.domain.category.entity;

import com.hhplus.ecommerce.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 자기 참조 연관관계 - 부모 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    // 자식 카테고리 목록 (양방향 매핑 - 선택적)
    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

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

    // 비즈니스 로직용 생성자
    public Category(Category parent, String name, String nameEn, Integer level,
                    Integer displayOrder, String imageUrl) {
        this.parent = parent;
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
        return this.parent == null;
    }

    // 부모 카테고리 설정
    public void setParent(Category parent) {
        this.parent = parent;
        if (parent != null && !parent.getChildren().contains(this)) {
            parent.getChildren().add(this);
        }
    }

    // 자식 카테고리 추가
    public void addChild(Category child) {
        this.children.add(child);
        child.parent = this;
    }
}
