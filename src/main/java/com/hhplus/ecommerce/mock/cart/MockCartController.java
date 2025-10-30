package com.hhplus.ecommerce.mock.cart;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 장바구니 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/carts")
public class MockCartController {

    /**
     * 장바구니 조회
     */
    @GetMapping("/me")
    public Map<String, Object> getCart(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if(CollectionUtils.isEmpty(carts)) {
            carts = new HashMap<>(Map.of(
                    "cartId", InMemoryDataStore.nextCartId(),
                    "userId", userId,
                    "createdAt", LocalDateTime.now().toString(),
                    "updatedAt", LocalDateTime.now().toString()
            ));
            InMemoryDataStore.CARTS.put(userId, carts);
        }

        Long cartId = (Long)carts.get("cartId");
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, new ArrayList<>());

        // 상품 정보와 함께 반환
        List<Map<String, Object>> enrichedItems = new ArrayList<>();
        long totalAmount = 0;
        int totalQuantity = 0;

        for (Map<String, Object> item : cartItems) {
            Long productId = (Long) item.get("productId");
            Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

            if (product != null) {
                Map<String, Object> enrichedItem = new HashMap<>(item);
                enrichedItem.put("cartItemId", item.get("cartItemId"));
                enrichedItem.put("productId", productId);
                enrichedItem.put("productName", product.get("name"));
                enrichedItem.put("productImageUrl", product.get("imageUrl"));
                enrichedItem.put("price", product.get("price"));

                int quantity = (int) item.get("quantity");
                long price = (Long) product.get("price");
                long subtotal = price * quantity;

                enrichedItem.put("subtotal", subtotal);

                Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get((Long)product.get("productId"));
                int stock = (int) inventory.get("stock");
                int reserved = (int) inventory.get("reservedStock");

                enrichedItem.put("availableStock", stock - reserved);
                enrichedItem.put("createdAt", item.get("createdAt"));

                enrichedItems.add(enrichedItem);

                totalQuantity += quantity;
                totalAmount += subtotal;
            }
        }

        return Map.of(
                "cartId", cartId,
                "userId", userId,
                "items", enrichedItems,
                "totalQuantity", totalQuantity,
                "totalAmount", totalAmount
        );
    }

    /**
     * 장바구니에 상품 추가
     */
    @PostMapping("/items")
    public Map<String, Object> addCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request
    ) {
        Long productId = ((Number) request.get("productId")).longValue();
        int quantity = ((Number) request.get("quantity")).intValue();

        // 상품 존재 확인
        Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);
        if (product == null) {
            return Map.of(
                    "code", "PRODUCT_NOT_FOUND",
                    "message", "상품을 찾을 수 없습니다"
            );
        }

        // 재고 확인
        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        int stock = (int) inventory.get("stock");

        if ("OUT_OF_STOCK".equals(product.get("status")) || stock == 0) {
            return Map.of("code", "PRODUCT_OUT_OF_STOCK",
                    "message", "품절된 상품입니다");
        }

        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if(CollectionUtils.isEmpty(carts)) {
            carts = new HashMap<>(Map.of(
                    "cartId", InMemoryDataStore.nextCartId(),
                    "userId", userId,
                    "createdAt", LocalDateTime.now().toString(),
                    "updatedAt", LocalDateTime.now().toString()
            ));
            InMemoryDataStore.CARTS.put(userId, carts);
        }

        Long cartId = (Long)carts.get("cartId");
        // 장바구니 아이템 목록 가져오기
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.getOrDefault(cartId, new ArrayList<>());

        // 이미 존재하는 상품인지 확인
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
            // 수량 업데이트
            int currentQty = (int) existingItem.get("quantity");
            existingItem.put("quantity", currentQty + quantity);
            existingItem.put("updatedAt", LocalDateTime.now().toString());

            quantity +=currentQty;

            return Map.of(
                    "cartItemId", existingItem.get("cartItemId"),
                    "productId", productId,
                    "productName",productName,
                    "quantity",  quantity,
                    "subtotal", price * quantity,
                    "createdAt", existingItem.get("createdAt")
            );
        } else {
            // 새로운 아이템 추가
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

            return Map.of(
                    "cartItemId", cartItemId,
                    "productId", productId,
                    "productName",productName,
                    "quantity", quantity,
                    "subtotal", price * quantity,
                    "createdAt", newItem.get("createdAt")
            );
        }
    }

    /**
     * 장바구니 아이템 수량 변경
     */
    @PatchMapping("/items/{cartItemId}")
    public Map<String, Object> updateCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> request
    ) {
        int quantity = ((Number) request.get("quantity")).intValue();

        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (CollectionUtils.isEmpty(carts)) {
            return Map.of("error", "장바구니가 비어있습니다");
        }
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.get((Long)carts.get("cartId"));
        if (CollectionUtils.isEmpty(cartItems)) {
            return Map.of("error", "장바구니가 비어있습니다");
        }

        for (Map<String, Object> item : cartItems) {
            if (cartItemId.equals(item.get("cartItemId"))) {
                Long productId = (Long) item.get("productId");
                String updatedAt = LocalDateTime.now().toString();
                item.put("quantity", quantity);
                item.put("updatedAt", updatedAt);

                Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);
                long price = (Long) product.get("price");

                return Map.of(
                        "cartItemId", cartItemId,
                        "productId", productId,
                        "quantity", quantity,
                        "subtotal", quantity * price,
                        "updatedAt", updatedAt
                );
            }
        }

        return Map.of("code", "CART_ITEM_NOT_FOUND",
                "message", "장바구니 항목을 찾을 수 없습니다");
    }

    /**
     * 장바구니 아이템 삭제
     */
    @DeleteMapping("/items/{cartItemId}")
    public Map<String, Object> deleteCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId
    ) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if(CollectionUtils.isEmpty(carts)) {
            return Map.of("error", "장바구니가 비어있습니다");
        }
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.get((Long)carts.get("cartId"));
        if (cartItems == null) {
            return Map.of("error", "장바구니가 비어있습니다");
        }

        boolean removed = cartItems.removeIf(item -> cartItemId.equals(item.get("cartItemId")));

        if (removed) {
            return Map.of();
        } else {
            return Map.of("code", "CART_ITEM_NOT_FOUND",
                    "message", "장바구니 항목을 찾을 수 없습니다");
        }
    }

    /**
     * 장바구니 전체 비우기
     */
    @DeleteMapping("/items")
    public Map<String, Object> clearCart(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if(!CollectionUtils.isEmpty(carts)) {
            InMemoryDataStore.CART_ITEMS.remove((Long)carts.get("cartId"));
        }
        return Map.of();
    }
}
