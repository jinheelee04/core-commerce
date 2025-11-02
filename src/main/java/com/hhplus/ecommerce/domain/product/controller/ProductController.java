package com.hhplus.ecommerce.domain.product.controller;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.dto.ApiResponse;
import com.hhplus.ecommerce.global.common.dto.PageMeta;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        List<ProductResponse> products = new ArrayList<>();

        for (Map<String, Object> product : InMemoryDataStore.PRODUCTS.values()) {
            if (category != null && !category.equals(product.get("category"))) {
                continue;
            }

            if (keyword != null && !keyword.isEmpty()) {
                String name = ((String) product.get("name")).toLowerCase();
                String description = ((String) product.get("description")).toLowerCase();
                String keywordLower = keyword.toLowerCase();

                if (!name.contains(keywordLower) && !description.contains(keywordLower)) {
                    continue;
                }
            }

            Long price = (Long) product.get("price");
            if (minPrice != null && price < minPrice) {
                continue;
            }
            if (maxPrice != null && price > maxPrice) {
                continue;
            }

            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get((Long) product.get("productId"));
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");

            ProductResponse productResponse = new ProductResponse(
                    (Long) product.get("productId"),
                    (String) product.get("name"),
                    (String) product.get("description"),
                    (Long) product.get("price"),
                    (String) product.get("category"),
                    (String) product.get("brand"),
                    (String) product.get("imageUrl"),
                    (String) product.get("status"),
                    null,
                    null,
                    stock - reserved,
                    (String) product.get("createdAt")
            );
            products.add(productResponse);
        }

        if (sort != null) {
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "asc";

            products.sort((p1, p2) -> {
                int comparison = 0;

                if ("price".equals(sortField)) {
                    comparison = p1.price().compareTo(p2.price());
                } else if ("createdAt".equals(sortField)) {
                    String date1 = p1.createdAt() != null ? p1.createdAt() : "";
                    String date2 = p2.createdAt() != null ? p2.createdAt() : "";
                    comparison = date1.compareTo(date2);
                }

                return "desc".equals(sortDirection) ? -comparison : comparison;
            });
        }

        int totalElements = products.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ProductResponse> pagedProducts = fromIndex < totalElements
                ? products.subList(fromIndex, toIndex)
                : new ArrayList<>();

        PageMeta pageMeta = new PageMeta(page, size, totalElements, totalPages);
        return ApiResponse.of(pagedProducts, pageMeta);
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);

        if (product == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
        if (inventory != null) {
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");

            ProductResponse response = new ProductResponse(
                    (Long) product.get("productId"),
                    (String) product.get("name"),
                    (String) product.get("description"),
                    (Long) product.get("price"),
                    (String) product.get("category"),
                    (String) product.get("brand"),
                    (String) product.get("imageUrl"),
                    (String) product.get("status"),
                    stock,
                    reserved,
                    stock - reserved,
                    (String) product.get("createdAt")
            );
            return ApiResponse.of(response);
        }

        ProductResponse response = new ProductResponse(
                (Long) product.get("productId"),
                (String) product.get("name"),
                (String) product.get("description"),
                (Long) product.get("price"),
                (String) product.get("category"),
                (String) product.get("brand"),
                (String) product.get("imageUrl"),
                (String) product.get("status"),
                null,
                null,
                null,
                (String) product.get("createdAt")
        );
        return ApiResponse.of(response);
    }
}
