package com.hhplus.ecommerce.domain.cart.repository;

import com.hhplus.ecommerce.domain.cart.model.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {
    CartItem save(CartItem cartItem);
    Optional<CartItem> findById(Long id);
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    List<CartItem> findAll();
    void deleteById(Long id);
    void deleteByCartId(Long cartId);
    Long generateNextId();
}
