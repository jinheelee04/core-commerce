package com.hhplus.ecommerce.domain.cart.repository;

import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryCartItemRepository implements CartItemRepository {

    @Override
    public CartItem save(CartItem cartItem) {
        List<CartItem> items = InMemoryDataStore.CART_ITEMS
                .computeIfAbsent(cartItem.getCartId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        items.removeIf(item -> item.getId().equals(cartItem.getId()));
        items.add(cartItem);

        return cartItem;
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        return InMemoryDataStore.CART_ITEMS.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<CartItem> findByCartId(Long cartId) {
        return InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, List.of());
    }

    @Override
    public Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId) {
        return InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, List.of()).stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    @Override
    public List<CartItem> findAll() {
        return InMemoryDataStore.CART_ITEMS.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.CART_ITEMS.values().forEach(list ->
                list.removeIf(item -> item.getId().equals(id))
        );
    }

    @Override
    public void deleteByCartId(Long cartId) {
        InMemoryDataStore.CART_ITEMS.remove(cartId);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.cartItemIdSequence.incrementAndGet();
    }
}
