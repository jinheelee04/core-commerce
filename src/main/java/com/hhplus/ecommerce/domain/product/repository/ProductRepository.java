package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll();
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategoryAndStatus(ProductCategory category, ProductStatus status);
    void deleteById(Long id);
    Long generateNextId();
}
