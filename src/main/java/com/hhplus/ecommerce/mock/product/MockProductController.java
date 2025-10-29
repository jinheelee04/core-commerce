package com.hhplus.ecommerce.mock.product;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 상품 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/products")
public class MockProductController {

    /**
     * 상품 목록 조회
     * GET /api/products
     */
    @GetMapping
    public Map<String, Object> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice
    ) {
        List<Map<String, Object>> products = new ArrayList<>();

        for (Map<String, Object> product : InMemoryDataStore.PRODUCTS.values()) {
            // 카테고리 필터
            if (category != null && !category.equals(product.get("category"))) {
                continue;
            }

            // 가격 범위 필터
            Long price = (Long) product.get("price");
            if (minPrice != null && price < minPrice) {
                continue;
            }
            if (maxPrice != null && price > maxPrice) {
                continue;
            }

            products.add(product);
        }

        return Map.of(
            "products", products,
            "total", products.size()
        );
    }

    /**
     * 상품 상세 조회
     * GET /api/products/{productId}
     */
    @GetMapping("/{productId}")
    public Map<String, Object> getProduct(@PathVariable Long productId) {
        Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

        if (product == null) {
            return Map.of("error", "상품을 찾을 수 없습니다");
        }

        // 재고 정보 추가
        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        if (inventory != null) {
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");
            Map<String, Object> response = new HashMap<>(product);
            response.put("availableStock", stock - reserved);
            return response;
        }

        return product;
    }
}
