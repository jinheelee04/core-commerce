package com.hhplus.ecommerce.domain.cart.controller;

import com.hhplus.ecommerce.domain.cart.dto.*;
import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.dto.ApiResponse;
import com.hhplus.ecommerce.global.common.enums.ProductStatus;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    @GetMapping("/me")
    public ApiResponse<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (CollectionUtils.isEmpty(carts)) {
            carts = new HashMap<>(Map.of(
                    "cartId", InMemoryDataStore.nextCartId(),
                    "userId", userId,
                    "createdAt", LocalDateTime.now().toString(),
                    "updatedAt", LocalDateTime.now().toString()
            ));
            InMemoryDataStore.CARTS.put(userId, carts);
        }

        Long cartId = (Long) carts.get("cartId");
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, new ArrayList<>());

        List<CartItemResponse> enrichedItems = new ArrayList<>();
        long totalAmount = 0;
        int totalQuantity = 0;

        for (Map<String, Object> item : cartItems) {
            Long productId = (Long) item.get("productId");
            Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

            if (product != null) {
                int quantity = (int) item.get("quantity");
                long price = (Long) product.get("price");
                long subtotal = price * quantity;

                Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get((Long) product.get("productId"));
                int stock = (int) inventory.get("stock");
                int reserved = (int) inventory.get("reservedStock");

                CartItemResponse itemResponse = new CartItemResponse(
                        (Long) item.get("cartItemId"),
                        productId,
                        (String) product.get("name"),
                        (String) product.get("imageUrl"),
                        price,
                        quantity,
                        subtotal,
                        stock - reserved,
                        (String) item.get("createdAt")
                );

                enrichedItems.add(itemResponse);
                totalQuantity += quantity;
                totalAmount += subtotal;
            }
        }

        CartResponse cartResponse = new CartResponse(
                cartId,
                userId,
                enrichedItems,
                totalQuantity,
                totalAmount,
                (String) carts.get("updatedAt")
        );

        return ApiResponse.of(cartResponse);
    }

    @PostMapping("/items")
    public ApiResponse<CartItemAddResponse> addCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request
    ) {
        Long productId = ((Number) request.get("productId")).longValue();
        int quantity = ((Number) request.get("quantity")).intValue();

        Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);
        if (product == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        int stock = (int) inventory.get("stock");

        if (ProductStatus.OUT_OF_STOCK.name().equals(product.get("status")) || stock == 0) {
            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
        }

        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (CollectionUtils.isEmpty(carts)) {
            carts = new HashMap<>(Map.of(
                    "cartId", InMemoryDataStore.nextCartId(),
                    "userId", userId,
                    "createdAt", LocalDateTime.now().toString(),
                    "updatedAt", LocalDateTime.now().toString()
            ));
            InMemoryDataStore.CARTS.put(userId, carts);
        }

        Long cartId = (Long) carts.get("cartId");
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, new ArrayList<>());

        Map<String, Object> existingItem = null;
        for (Map<String, Object> item : cartItems) {
            if (productId.equals(item.get("productId"))) {
                existingItem = item;
                break;
            }
        }

        String productName = (String) product.get("name");
        long price = (Long) product.get("price");

        if (existingItem != null) {
            int currentQty = (int) existingItem.get("quantity");
            existingItem.put("quantity", currentQty + quantity);
            existingItem.put("updatedAt", LocalDateTime.now().toString());

            quantity += currentQty;

            CartItemAddResponse response = new CartItemAddResponse(
                    (Long) existingItem.get("cartItemId"),
                    productId,
                    productName,
                    quantity,
                    price * quantity,
                    (String) existingItem.get("createdAt")
            );
            return ApiResponse.of(response);
        } else {
            Long cartItemId = InMemoryDataStore.nextCartItemId();
            Map<String, Object> newItem = new HashMap<>(Map.of(
                    "cartItemId", cartItemId,
                    "cartId", cartId,
                    "productId", productId,
                    "quantity", quantity,
                    "createdAt", LocalDateTime.now().toString(),
                    "updatedAt", LocalDateTime.now().toString()
            ));
            cartItems.add(newItem);
            InMemoryDataStore.CART_ITEMS.put(cartId, cartItems);

            CartItemAddResponse response = new CartItemAddResponse(
                    cartItemId,
                    productId,
                    productName,
                    quantity,
                    price * quantity,
                    (String) newItem.get("createdAt")
            );
            return ApiResponse.of(response);
        }
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartItemAddResponse> updateCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> request
    ) {
        int quantity = ((Number) request.get("quantity")).intValue();

        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (CollectionUtils.isEmpty(carts)) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.get((Long) carts.get("cartId"));
        if (CollectionUtils.isEmpty(cartItems)) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        for (Map<String, Object> item : cartItems) {
            if (cartItemId.equals(item.get("cartItemId"))) {
                Long productId = (Long) item.get("productId");
                String updatedAt = LocalDateTime.now().toString();
                item.put("quantity", quantity);
                item.put("updatedAt", updatedAt);

                Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);
                long price = (Long) product.get("price");

                CartItemAddResponse response = new CartItemAddResponse(
                        cartItemId,
                        productId,
                        null,
                        quantity,
                        quantity * price,
                        updatedAt
                );
                return ApiResponse.of(response);
            }
        }

        throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Object> deleteCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId
    ) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (CollectionUtils.isEmpty(carts)) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.get((Long) carts.get("cartId"));
        if (cartItems == null) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        boolean removed = cartItems.removeIf(item -> cartItemId.equals(item.get("cartItemId")));

        if (!removed) {
            throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        return ApiResponse.empty();
    }

    @DeleteMapping("/items")
    public ApiResponse<Object> clearCart(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (!CollectionUtils.isEmpty(carts)) {
            InMemoryDataStore.CART_ITEMS.remove((Long) carts.get("cartId"));
        }
        return ApiResponse.empty();
    }
}