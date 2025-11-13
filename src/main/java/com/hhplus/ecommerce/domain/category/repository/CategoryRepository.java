package com.hhplus.ecommerce.domain.category.repository;

import com.hhplus.ecommerce.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentId(Long parentId);

    List<Category> findByLevel(Integer level);

    List<Category> findByIsActiveTrueOrderByDisplayOrder();

    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrder(Long parentId);
}
