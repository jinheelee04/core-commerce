package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> findByDynamicFilters(Long categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findPopularProducts(String sortBy, Pageable pageable);
}
