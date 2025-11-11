package com.hhplus.ecommerce.domain.product.controller;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.entity.ProductStatus;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.dto.CommonResponse;
import com.hhplus.ecommerce.global.dto.ErrorResponse;
import com.hhplus.ecommerce.global.dto.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "상품 API", description = "상품 조회 관련 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 조회", description = "카테고리, 상태, 정렬로 필터링하여 상품 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<ProductResponse>>> getProducts(
            @Parameter(description = "카테고리 ID", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "상품 상태", example = "ACTIVE")
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        PagedResult<ProductResponse> result = productService.getProducts(categoryId, status, pageable);
        return ResponseEntity.ok(CommonResponse.success(result.content(), result.meta()));
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다 (조회수 증가)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{productId}")
    public ResponseEntity<CommonResponse<ProductResponse>> getProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId) {
        ProductResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(
            summary = "인기 상품 조회",
            description = "조회수, 판매량 또는 종합 점수 기준으로 인기 상품 목록을 조회합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/popular")
    public ResponseEntity<CommonResponse<List<ProductResponse>>> getPopularProducts(
            @Parameter(description = "정렬 기준 (views: 조회수, sales: 판매량)", example = "views")
            @RequestParam(required = false, defaultValue = "views") String sortBy,
            @PageableDefault(size = 10, sort = "viewCount") Pageable pageable
    ) {
        PagedResult<ProductResponse> result = productService.getPopularProducts(sortBy, pageable);
        return ResponseEntity.ok(CommonResponse.success(result.content(), result.meta()));
    }
}
