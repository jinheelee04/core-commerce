package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository In-Memory 구현
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    @Override
    public Product save(Product product) {
        InMemoryDataStore.PRODUCTS.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.PRODUCTS.get(id));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(InMemoryDataStore.PRODUCTS.values());
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        return InMemoryDataStore.PRODUCTS.values().stream()
                .filter(p -> p.getCategory() == category)
                .toList();
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return InMemoryDataStore.PRODUCTS.values().stream()
                .filter(p -> p.getStatus() == status)
                .toList();
    }

    @Override
    public List<Product> findByCategoryAndStatus(ProductCategory category, ProductStatus status) {
        return InMemoryDataStore.PRODUCTS.values().stream()
                .filter(p -> p.getCategory() == category && p.getStatus() == status)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.PRODUCTS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.productIdSequence.incrementAndGet();
    }
}
