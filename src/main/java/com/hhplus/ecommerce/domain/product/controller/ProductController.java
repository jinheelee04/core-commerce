package com.hhplus.ecommerce.domain.product.controller;

import com.hhplus.ecommerce.domain.product.dto.ProductResponse;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.common.dto.ErrorResponse;
import com.hhplus.ecommerce.global.common.dto.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
            @Parameter(description = "카테고리 필터", example = "ELECTRONICS")
            @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "상품 상태", example = "AVAILABLE")
            @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "정렬 기준", example = "price,asc")
            @RequestParam(required = false, defaultValue = "created,desc") String sort,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {

        PagedResult<ProductResponse> result = productService.getProducts(category, status, sort, page, size);
        return ResponseEntity.ok(CommonResponse.of(result.content(), result.meta()));
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
    public ResponseEntity<CommonResponse<ProductResponse>> getProduct(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Long productId) {
        ProductResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
