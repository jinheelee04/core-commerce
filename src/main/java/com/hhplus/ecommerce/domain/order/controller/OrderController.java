package com.hhplus.ecommerce.domain.order.controller;

import com.hhplus.ecommerce.domain.order.dto.*;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.global.dto.CommonResponse;
import com.hhplus.ecommerce.global.dto.PagedResult;
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

import java.util.List;

@Tag(name = "주문 API", description = "주문 생성 및 관리 관련 API")
@SecurityRequirement(name = SecurityConstants.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "장바구니 항목들로 주문을 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            @ApiResponse(responseCode = "400", description = "빈 장바구니 또는 쿠폰 오류"),
            @ApiResponse(responseCode = "409", description = "재고 부족")
    })
    @PostMapping
    public ResponseEntity<CommonResponse<OrderResponse>> createOrder(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(
                userId,
                request.cartItemIds(),
                request.couponId(),
                request.deliveryAddress(),
                request.deliveryMemo()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(response));
    }

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 상세 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<OrderResponse>> getOrder(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @PathVariable Long id
    ) {
        OrderResponse response = orderService.getOrder(userId, id);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<OrderSummaryResponse>>> getOrders(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        PagedResult<OrderSummaryResponse> result = orderService.getUserOrders(userId, page, size);
        return ResponseEntity.ok(CommonResponse.success(result.content(), result.meta()));
    }

    @Operation(summary = "주문 취소", description = "주문을 취소합니다 (재고 복구, 쿠폰 복구)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가능한 상태"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<CommonResponse<CancelOrderResponse>> cancelOrder(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        CancelOrderResponse response = orderService.cancelOrder(userId, id, request.cancelReason());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

}
