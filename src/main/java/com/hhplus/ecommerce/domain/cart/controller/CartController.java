package com.hhplus.ecommerce.domain.cart.controller;

import com.hhplus.ecommerce.domain.cart.dto.AddCartItemRequest;
import com.hhplus.ecommerce.domain.cart.dto.CartItemAddResponse;
import com.hhplus.ecommerce.domain.cart.dto.CartResponse;
import com.hhplus.ecommerce.domain.cart.dto.UpdateCartItemRequest;
import com.hhplus.ecommerce.domain.cart.service.CartService;
import com.hhplus.ecommerce.global.dto.CommonResponse;
import com.hhplus.ecommerce.global.constants.HttpHeaders;
import com.hhplus.ecommerce.global.constants.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장바구니 API", description = "장바구니 관리 관련 API")
@SecurityRequirement(name = SecurityConstants.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<CartResponse>> getCart(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId
    ) {
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "장바구니에 상품 담기", description = "장바구니에 상품을 추가합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 추가 성공"),
            @ApiResponse(responseCode = "400", description = "품절 상품")
    })
    @PostMapping("/items")
    public ResponseEntity<CommonResponse<CartItemAddResponse>> addCartItem(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        CartItemAddResponse response = cartService.addItem(userId, request.productId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(response));
    }

    @Operation(summary = "장바구니 수량 변경", description = "장바구니 항목의 수량을 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CommonResponse<CartItemAddResponse>> updateCartItem(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        CartItemAddResponse response = cartService.updateItemQuantity(userId, cartItemId, request.quantity());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "장바구니 항목 삭제", description = "장바구니에서 특정 항목을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId
    ) {
        cartService.removeItem(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "장바구니 비우기", description = "장바구니의 모든 항목을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비우기 성공")
    })
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId
    ) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
