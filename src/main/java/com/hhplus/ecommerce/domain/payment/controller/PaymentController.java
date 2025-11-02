package com.hhplus.ecommerce.domain.payment.controller;

import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.payment.dto.PaymentRequest;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "결제 API", description = "결제 처리 관련 API")
@SecurityRequirement(name = "X-User-Id")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private RestClient restClient;

    @Value("${mock.payment.url}")
    private String mockPaymentUrl;

    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 처리합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 성공"),
            @ApiResponse(responseCode = "400", description = "주문 상태 오류 또는 금액 불일치"),
            @ApiResponse(responseCode = "408", description = "결제 타임아웃")
    })
    @PostMapping
    public CommonResponse<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        Long orderId = request.orderId();
        String paymentMethod = request.paymentMethod();

        // 1. 주문 조회
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        String orderStatus = (String) order.get("status");
        if (!"PENDING".equals(orderStatus)) {
            throw new BusinessException(PaymentErrorCode.INVALID_ORDER_STATUS,
                Map.of(
                    "orderId", orderId,
                    "currentStatus", orderStatus
                ));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> pricing = (Map<String, Object>) order.get("pricing");
        Long itemsTotal = (Long) pricing.get("itemsTotal");
        Long discountAmount = (Long) pricing.get("discountAmount");
        Long finalAmount = (Long) pricing.get("finalAmount");

        // 2. Mock PG 호출
        Map<String, Object> pgRequest = Map.of(
            "orderId", orderId,
            "amount", finalAmount,
            "paymentMethod", paymentMethod
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> pgResponse = restClient.post()
            .uri(mockPaymentUrl)
            .body(pgRequest)
            .retrieve()
            .body(Map.class);

        if (pgResponse == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_FAILED);
        }

        String pgStatus = (String) pgResponse.get("status");

        // 3. 결제 결과 처리
        Long paymentId = InMemoryDataStore.nextPaymentId();
        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("orderId", orderId);
        payment.put("amount", itemsTotal);
        payment.put("discountAmount", discountAmount);
        payment.put("finalAmount", finalAmount);
        payment.put("paymentMethod", paymentMethod);
        payment.put("status", pgStatus);
        payment.put("createdAt", LocalDateTime.now().toString());

        if ("SUCCESS".equals(pgStatus)) {
            // 결제 성공 처리
            handlePaymentSuccess(order, payment, pgResponse);
        } else {
            // 결제 실패 처리
            handlePaymentFailure(order, payment, pgResponse);
        }

        InMemoryDataStore.PAYMENTS.put(paymentId, payment);

        return CommonResponse.of(toPaymentResponse(payment));
    }

    private void handlePaymentSuccess(Map<String, Object> order, Map<String, Object> payment, Map<String, Object> pgResponse) {
        payment.put("transactionId", pgResponse.get("transactionId"));
        payment.put("paidAt", pgResponse.get("paidAt"));

        order.put("status", "PAID");
        order.put("paidAt", pgResponse.get("paidAt"));

        // 재고 확정 차감
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        for (Map<String, Object> item : items) {
            Long productId = (Long) item.get("productId");
            int quantity = (int) item.get("quantity");

            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");

            inventory.put("stock", stock - quantity);
            inventory.put("reservedStock", reserved - quantity);
        }

        // 쿠폰 사용 처리
        @SuppressWarnings("unchecked")
        Map<String, Object> appliedCoupon = (Map<String, Object>) order.get("coupon");
        if (appliedCoupon != null) {
            Long userId = (Long) order.get("userId");
            Long couponMasterId = (Long) appliedCoupon.get("couponId");
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.get(userId);

            if (userCoupons != null) {
                for (Map<String, Object> userCoupon : userCoupons) {
                    if (couponMasterId.equals(userCoupon.get("couponId")) &&
                        !((Boolean) userCoupon.getOrDefault("isUsed", false))) {
                        userCoupon.put("isUsed", true);
                        userCoupon.put("usedAt", LocalDateTime.now().toString());
                        userCoupon.put("orderId", order.get("orderId"));
                        break;
                    }
                }
            }
        }
    }

    private void handlePaymentFailure(Map<String, Object> order, Map<String, Object> payment, Map<String, Object> pgResponse) {
        payment.put("failReason", pgResponse.get("failReason"));
        payment.put("failedAt", pgResponse.get("failedAt"));

        order.put("status", "CANCELLED");
        order.put("cancelReason", "결제 실패: " + pgResponse.get("failReason"));
        order.put("cancelledAt", pgResponse.get("failedAt"));

        // 재고 예약 해제
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        for (Map<String, Object> item : items) {
            Long productId = (Long) item.get("productId");
            int quantity = (int) item.get("quantity");

            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
            int reserved = (int) inventory.get("reservedStock");
            inventory.put("reservedStock", Math.max(0, reserved - quantity));
        }
    }

    @Operation(summary = "결제 상세 조회", description = "결제 ID로 결제 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
    })
    @GetMapping("/{paymentId}")
    public CommonResponse<PaymentResponse> getPayment(
            @Parameter(description = "결제 ID", example = "789", required = true)
            @PathVariable Long paymentId) {
        Map<String, Object> payment = InMemoryDataStore.PAYMENTS.get(paymentId);
        if (payment == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return CommonResponse.of(toPaymentResponse(payment));
    }

    @Operation(summary = "주문별 결제 조회 (deprecated)", description = "주문 ID로 결제 정보를 조회합니다. GET /orders/{orderId}/payment 사용을 권장합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "결제 내역 없음")
    })
    @GetMapping("/order/{orderId}")
    public CommonResponse<PaymentResponse> getPaymentByOrder(
            @Parameter(description = "주문 ID", example = "456", required = true)
            @PathVariable Long orderId) {
        for (Map<String, Object> payment : InMemoryDataStore.PAYMENTS.values()) {
            if (orderId.equals(payment.get("orderId"))) {
                return CommonResponse.of(toPaymentResponse(payment));
            }
        }
        throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    private PaymentResponse toPaymentResponse(Map<String, Object> payment) {
        return new PaymentResponse(
                (Long) payment.get("paymentId"),
                (Long) payment.get("orderId"),
                (Long) payment.get("amount"),
                (Long) payment.get("discountAmount"),
                (Long) payment.get("finalAmount"),
                (String) payment.get("paymentMethod"),
                (String) payment.get("status"),
                (String) payment.get("transactionId"),
                (String) payment.get("failReason"),
                (String) payment.get("paidAt"),
                (String) payment.get("failedAt"),
                (String) payment.get("createdAt")
        );
    }
}
