package com.hhplus.ecommerce.mock.payment;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 결제 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/payments")
public class MockPaymentController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${mock.payment.url}")
    private String mockPaymentUrl;

    /**
     * 결제 요청
     * POST /api/payments
     */
    @PostMapping
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> request) {
        Long orderId = ((Number) request.get("orderId")).longValue();
        String paymentMethod = (String) request.get("paymentMethod");

        // 1. 주문 조회
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            return Map.of(
                "code", "ORDER_NOT_FOUND",
                "message", "주문을 찾을 수 없습니다"
            );
        }

        String orderStatus = (String) order.get("status");
        if (!"PENDING".equals(orderStatus)) {
            return Map.of(
                "code", "INVALID_ORDER_STATUS",
                "message", "결제 대기 상태의 주문만 결제할 수 있습니다",
                "details", Map.of(
                    "orderId", orderId,
                    "currentStatus", orderStatus
                )
            );
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
        Map<String, Object> pgResponse = restTemplate.postForObject(
            mockPaymentUrl, pgRequest, Map.class
        );

        if (pgResponse == null) {
            return Map.of(
                "code", "PAYMENT_FAILED",
                "message", "결제 처리 중 오류가 발생했습니다"
            );
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

        return Map.of("data", payment);
    }

    /**
     * 결제 성공 처리
     * - 주문 상태 변경 (PENDING -> PAID)
     * - 재고 확정 차감
     * - 쿠폰 사용 처리
     */
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

    /**
     * 결제 실패 처리
     * - 주문 상태 변경 (PENDING -> CANCELLED)
     * - 재고 예약 해제
     */
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

    /**
     * 결제 조회
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public Map<String, Object> getPayment(@PathVariable Long paymentId) {
        Map<String, Object> payment = InMemoryDataStore.PAYMENTS.get(paymentId);
        if (payment == null) {
            return Map.of(
                "code", "PAYMENT_NOT_FOUND",
                "message", "결제 정보를 찾을 수 없습니다"
            );
        }
        return Map.of("data", payment);
    }

    /**
     * 주문별 결제 조회
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getPaymentByOrder(@PathVariable Long orderId) {
        for (Map<String, Object> payment : InMemoryDataStore.PAYMENTS.values()) {
            if (orderId.equals(payment.get("orderId"))) {
                return Map.of("data", payment);
            }
        }
        return Map.of(
            "code", "PAYMENT_NOT_FOUND",
            "message", "해당 주문의 결제 내역이 없습니다"
        );
    }
}
