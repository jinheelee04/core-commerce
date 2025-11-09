package com.hhplus.ecommerce.domain.cart.repository;

import com.hhplus.ecommerce.domain.cart.model.Cart;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryCartRepository implements CartRepository {

    @Override
    public Cart save(Cart cart) {
        InMemoryDataStore.CARTS.put(cart.getId(), cart);
        return cart;
    }

    @Override
    public Optional<Cart> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.CARTS.get(id));
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return InMemoryDataStore.CARTS.values().stream()
                .filter(cart -> cart.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<Cart> findAll() {
        return List.copyOf(InMemoryDataStore.CARTS.values());
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.CARTS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.cartIdSequence.incrementAndGet();
    }
}
