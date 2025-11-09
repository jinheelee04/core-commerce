package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.model.Inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(Long id);
    Optional<Inventory> findByProductId(Long productId);
    List<Inventory> findAll();
    List<Inventory> findLowStockProducts();
    void deleteById(Long id);
    Long generateNextId();
}
