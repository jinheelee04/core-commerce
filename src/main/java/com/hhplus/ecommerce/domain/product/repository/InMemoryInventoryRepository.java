package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 재고 Repository In-Memory 구현
 */
@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    @Override
    public Inventory save(Inventory inventory) {
        InMemoryDataStore.INVENTORY.put(inventory.getId(), inventory);
        return inventory;
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.INVENTORY.get(id));
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return InMemoryDataStore.INVENTORY.values().stream()
                .filter(inv -> inv.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public List<Inventory> findAll() {
        return List.copyOf(InMemoryDataStore.INVENTORY.values());
    }

    @Override
    public List<Inventory> findLowStockProducts() {
        return InMemoryDataStore.INVENTORY.values().stream()
                .filter(Inventory::isLowStock)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.INVENTORY.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.inventoryIdSequence.incrementAndGet();
    }
}
