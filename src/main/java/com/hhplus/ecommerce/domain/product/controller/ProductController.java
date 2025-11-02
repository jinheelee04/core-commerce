package com.hhplus.ecommerce.domain.product.controller;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.common.dto.ErrorResponse;
import com.hhplus.ecommerce.global.common.dto.PageMeta;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "상품 API", description = "상품 조회 관련 API")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Operation(summary = "상품 목록 조회", description = "카테고리, 키워드, 가격 범위로 필터링하여 상품 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public CommonResponse<List<ProductResponse>> getProducts(
            @Parameter(description = "카테고리 필터 (예: ELECTRONICS)", example = "ELECTRONICS")
            @RequestParam(required = false) String category,
            @Parameter(description = "검색 키워드 (상품명, 설명)", example = "MacBook")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "최소 가격", example = "1000000")
            @RequestParam(required = false) Long minPrice,
            @Parameter(description = "최대 가격", example = "3000000")
            @RequestParam(required = false) Long maxPrice,
            @Parameter(description = "정렬 기준 (price,asc | createdAt,desc)", example = "createdAt,desc")
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
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
        return CommonResponse.of(pagedProducts, pageMeta);
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{productId}")
    public CommonResponse<ProductResponse> getProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId) {
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
            return CommonResponse.of(response);
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
        return CommonResponse.of(response);
    }
}
