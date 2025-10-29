package com.hhplus.ecommerce.mock.cart;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
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
     * GET /api/carts
     */
    @GetMapping
    public Map<String, Object> getCart(@RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> cartItems = InMemoryDataStore.CARTS.getOrDefault(userId, new ArrayList<>());

        // 상품 정보와 함께 반환
        List<Map<String, Object>> enrichedItems = new ArrayList<>();
        long totalAmount = 0;

        for (Map<String, Object> item : cartItems) {
            Long productId = (Long) item.get("productId");
            Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

            if (product != null) {
                Map<String, Object> enrichedItem = new HashMap<>(item);
                enrichedItem.put("productName", product.get("name"));
                enrichedItem.put("productPrice", product.get("price"));

                int quantity = (int) item.get("quantity");
                long price = (Long) product.get("price");
                long subtotal = price * quantity;

                enrichedItem.put("subtotal", subtotal);
                enrichedItems.add(enrichedItem);

                totalAmount += subtotal;
            }
        }

        return Map.of(
            "items", enrichedItems,
            "totalAmount", totalAmount,
            "itemCount", enrichedItems.size()
        );
    }

    /**
     * 장바구니에 상품 추가
     * POST /api/carts/items
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
            return Map.of("error", "상품을 찾을 수 없습니다");
        }

        // 재고 확인
        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        int stock = (int) inventory.get("stock");
        int reserved = (int) inventory.get("reservedStock");
        if (stock - reserved < quantity) {
            return Map.of("error", "재고가 부족합니다");
        }

        // 장바구니 아이템 목록 가져오기
        List<Map<String, Object>> cartItems = InMemoryDataStore.CARTS.computeIfAbsent(
            userId, k -> new ArrayList<>()
        );

        // 이미 존재하는 상품인지 확인
        Map<String, Object> existingItem = null;
        for (Map<String, Object> item : cartItems) {
            if (productId.equals(item.get("productId"))) {
                existingItem = item;
                break;
            }
        }

        if (existingItem != null) {
            // 수량 업데이트
            int currentQty = (int) existingItem.get("quantity");
            existingItem.put("quantity", currentQty + quantity);
            existingItem.put("updatedAt", LocalDateTime.now().toString());

            return Map.of(
                "cartItemId", existingItem.get("cartItemId"),
                "productId", productId,
                "quantity", currentQty + quantity,
                "message", "장바구니 수량이 업데이트되었습니다"
            );
        } else {
            // 새로운 아이템 추가
            Long cartItemId = InMemoryDataStore.nextCartItemId();
            Map<String, Object> newItem = new HashMap<>(Map.of(
                "cartItemId", cartItemId,
                "productId", productId,
                "quantity", quantity,
                "createdAt", LocalDateTime.now().toString()
            ));
            cartItems.add(newItem);

            return Map.of(
                "cartItemId", cartItemId,
                "productId", productId,
                "quantity", quantity,
                "message", "장바구니에 추가되었습니다"
            );
        }
    }

    /**
     * 장바구니 아이템 수량 변경
     * PUT /api/carts/items/{cartItemId}
     */
    @PutMapping("/items/{cartItemId}")
    public Map<String, Object> updateCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> request
    ) {
        int quantity = ((Number) request.get("quantity")).intValue();

        List<Map<String, Object>> cartItems = InMemoryDataStore.CARTS.get(userId);
        if (cartItems == null) {
            return Map.of("error", "장바구니가 비어있습니다");
        }

        for (Map<String, Object> item : cartItems) {
            if (cartItemId.equals(item.get("cartItemId"))) {
                Long productId = (Long) item.get("productId");

                // 재고 확인
                Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
                int stock = (int) inventory.get("stock");
                int reserved = (int) inventory.get("reservedStock");
                if (stock - reserved < quantity) {
                    return Map.of("error", "재고가 부족합니다");
                }

                item.put("quantity", quantity);
                item.put("updatedAt", LocalDateTime.now().toString());

                return Map.of(
                    "cartItemId", cartItemId,
                    "quantity", quantity,
                    "message", "수량이 변경되었습니다"
                );
            }
        }

        return Map.of("error", "장바구니 아이템을 찾을 수 없습니다");
    }

    /**
     * 장바구니 아이템 삭제
     * DELETE /api/carts/items/{cartItemId}
     */
    @DeleteMapping("/items/{cartItemId}")
    public Map<String, Object> deleteCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId
    ) {
        List<Map<String, Object>> cartItems = InMemoryDataStore.CARTS.get(userId);
        if (cartItems == null) {
            return Map.of("error", "장바구니가 비어있습니다");
        }

        boolean removed = cartItems.removeIf(item -> cartItemId.equals(item.get("cartItemId")));

        if (removed) {
            return Map.of("message", "장바구니에서 삭제되었습니다");
        } else {
            return Map.of("error", "장바구니 아이템을 찾을 수 없습니다");
        }
    }

    /**
     * 장바구니 전체 비우기
     * DELETE /api/carts
     */
    @DeleteMapping
    public Map<String, Object> clearCart(@RequestHeader("X-User-Id") Long userId) {
        InMemoryDataStore.CARTS.remove(userId);
        return Map.of("message", "장바구니가 비워졌습니다");
    }
}
