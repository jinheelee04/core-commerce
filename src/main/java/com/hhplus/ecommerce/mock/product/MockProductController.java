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
     */
    @GetMapping
    public Map<String, Object> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        List<Map<String, Object>> products = new ArrayList<>();

        for (Map<String, Object> product : InMemoryDataStore.PRODUCTS.values()) {
            // 카테고리 필터
            if (category != null && !category.equals(product.get("category"))) {
                continue;
            }

            // 키워드 검색 (상품명, 설명)
            if (keyword != null && !keyword.isEmpty()) {
                String name = ((String) product.get("name")).toLowerCase();
                String description = ((String) product.get("description")).toLowerCase();
                String keywordLower = keyword.toLowerCase();

                if (!name.contains(keywordLower) && !description.contains(keywordLower)) {
                    continue;
                }
            }

            // 가격 범위 필터
            Long price = (Long) product.get("price");
            if (minPrice != null && price < minPrice) {
                continue;
            }
            if (maxPrice != null && price > maxPrice) {
                continue;
            }

            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get((Long)product.get("productId"));
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");
            product.put("availableStock", stock - reserved);
            product.remove("updatedAt");
            products.add(product);
        }

        // 정렬 처리
        if (sort != null) {
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "asc";

            products.sort((p1, p2) -> {
                int comparison = 0;

                if ("price".equals(sortField)) {
                    Long price1 = (Long) p1.get("price");
                    Long price2 = (Long) p2.get("price");
                    comparison = price1.compareTo(price2);
                } else if ("createdAt".equals(sortField)) {
                    String date1 = (String) p1.getOrDefault("createdAt", "");
                    String date2 = (String) p2.getOrDefault("createdAt", "");
                    comparison = date1.compareTo(date2);
                }

                return "desc".equals(sortDirection) ? -comparison : comparison;
            });
        }

        // 페이징 처리
        int totalElements = products.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Map<String, Object>> pagedProducts = fromIndex < totalElements
            ? products.subList(fromIndex, toIndex)
            : new ArrayList<>();

        return Map.of(
            "data", pagedProducts,
            "meta", Map.of(
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
            )
        );
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public Map<String, Object> getProduct(@PathVariable Long productId) {
        Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

        if (product == null) {
            return Map.of(
                    "code", "PRODUCT_NOT_FOUND",
                    "message", "상품을 찾을 수 없습니다"

            );
        }

        // 재고 정보 추가
        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        if (inventory != null) {
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");
            Map<String, Object> response = new HashMap<>(product);
            response.put("stock", stock);
            response.put("reservedStock", reserved);
            response.put("availableStock", stock - reserved);
            return response;
        }

        return product;
    }
}
