package com.hhplus.ecommerce.domain.cart.controller;

import com.hhplus.ecommerce.domain.cart.dto.*;
import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.common.enums.ProductStatus;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "장바구니 API", description = "장바구니 관리 관련 API")
@SecurityRequirement(name = "X-User-Id")
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/me")
    public CommonResponse<CartResponse> getCart(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
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

        return CommonResponse.of(cartResponse);
    }

    @Operation(summary = "장바구니에 상품 담기", description = "장바구니에 상품을 추가합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 추가 성공"),
            @ApiResponse(responseCode = "400", description = "품절 상품")
    })
    @PostMapping("/items")
    public CommonResponse<CartItemAddResponse> addCartItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody AddCartItemRequest request
    ) {
        Long productId = request.productId();
        int quantity = request.quantity();

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
            return CommonResponse.of(response);
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
            return CommonResponse.of(response);
        }
    }

    @Operation(summary = "장바구니 수량 변경", description = "장바구니 항목의 수량을 변경합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @PatchMapping("/items/{cartItemId}")
    public CommonResponse<CartItemAddResponse> updateCartItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequest request
    ) {
        int quantity = request.quantity();

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
                return CommonResponse.of(response);
            }
        }

        throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Operation(summary = "장바구니 항목 삭제", description = "장바구니에서 특정 항목을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @DeleteMapping("/items/{cartItemId}")
    public CommonResponse<Object> deleteCartItem(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "장바구니 항목 ID", example = "1", required = true)
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

        return CommonResponse.empty();
    }

    @Operation(summary = "장바구니 비우기", description = "장바구니의 모든 항목을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비우기 성공")
    })
    @DeleteMapping("/items")
    public CommonResponse<Object> clearCart(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> carts = InMemoryDataStore.CARTS.getOrDefault(userId, new HashMap<>());
        if (!CollectionUtils.isEmpty(carts)) {
            InMemoryDataStore.CART_ITEMS.remove((Long) carts.get("cartId"));
        }
        return CommonResponse.empty();
    }
}