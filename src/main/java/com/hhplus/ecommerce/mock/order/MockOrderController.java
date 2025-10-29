package com.hhplus.ecommerce.mock.order;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 주문 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/orders")
public class MockOrderController {

    /**
     * 주문 생성
     * POST /api/orders
     * - 재고 예약 처리
     */
    @PostMapping
    public Map<String, Object> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        String deliveryAddress = (String) request.get("deliveryAddress");
        String deliveryMemo = (String) request.getOrDefault("deliveryMemo", "");
        String couponCode = (String) request.getOrDefault("couponCode", null);

        if (items == null || items.isEmpty()) {
            return Map.of("error", "주문 상품이 없습니다");
        }

        // 1. 재고 확인 및 예약
        List<Map<String, Object>> orderItems = new ArrayList<>();
        long itemsTotal = 0;

        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();

            // 상품 조회
            Map<String, Object> product = InMemoryDataStore.PRODUCTS.get(productId);
            if (product == null) {
                return Map.of("error", "상품을 찾을 수 없습니다", "productId", productId);
            }

            // 재고 확인
            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");

            if (stock - reserved < quantity) {
                return Map.of("error", "재고가 부족합니다", "productId", productId, "availableStock", stock - reserved);
            }

            // 재고 예약
            inventory.put("reservedStock", reserved + quantity);

            // 주문 항목 생성
            long unitPrice = (Long) product.get("price");
            long subtotal = unitPrice * quantity;

            orderItems.add(Map.of(
                "productId", productId,
                "productName", product.get("name"),
                "quantity", quantity,
                "unitPrice", unitPrice,
                "subtotal", subtotal
            ));

            itemsTotal += subtotal;
        }

        // 2. 쿠폰 할인 계산
        long discountAmount = 0;
        if (couponCode != null && !couponCode.isEmpty()) {
            Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(couponCode);

            if (coupon == null) {
                // 재고 예약 해제
                rollbackInventoryReservation(items);
                return Map.of("error", "유효하지 않은 쿠폰 코드입니다");
            }

            if (!"ACTIVE".equals(coupon.get("status"))) {
                rollbackInventoryReservation(items);
                return Map.of("error", "사용할 수 없는 쿠폰입니다");
            }

            // 사용자가 해당 쿠폰을 발급받았는지 확인
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.get(userId);
            boolean hasCoupon = false;
            if (userCoupons != null) {
                for (Map<String, Object> uc : userCoupons) {
                    if (couponCode.equals(uc.get("code")) && !((Boolean) uc.getOrDefault("isUsed", false))) {
                        hasCoupon = true;
                        break;
                    }
                }
            }

            if (!hasCoupon) {
                rollbackInventoryReservation(items);
                return Map.of("error", "발급받지 않은 쿠폰입니다");
            }

            // 최소 주문 금액 확인
            long minOrderAmount = (Long) coupon.get("minOrderAmount");
            if (itemsTotal < minOrderAmount) {
                rollbackInventoryReservation(items);
                return Map.of("error", "최소 주문 금액을 충족하지 않습니다", "minOrderAmount", minOrderAmount);
            }

            // 할인 금액 계산
            String discountType = (String) coupon.get("discountType");
            if ("PERCENTAGE".equals(discountType)) {
                int discountValue = (int) coupon.get("discountValue");
                discountAmount = itemsTotal * discountValue / 100;

                Long maxDiscountAmount = (Long) coupon.get("maxDiscountAmount");
                if (maxDiscountAmount != null) {
                    discountAmount = Math.min(discountAmount, maxDiscountAmount);
                }
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                discountAmount = (int) coupon.get("discountValue");
            }
        }

        long finalAmount = itemsTotal - discountAmount;

        // 3. 주문 생성
        Long orderId = InMemoryDataStore.nextOrderId();
        String orderNumber = InMemoryDataStore.generateOrderNumber();

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("userId", userId);
        order.put("orderNumber", orderNumber);
        order.put("status", "PENDING");
        order.put("itemsTotal", itemsTotal);
        order.put("discountAmount", discountAmount);
        order.put("finalAmount", finalAmount);
        order.put("deliveryAddress", deliveryAddress);
        order.put("deliveryMemo", deliveryMemo);
        order.put("items", orderItems);
        order.put("createdAt", LocalDateTime.now().toString());

        if (couponCode != null && !couponCode.isEmpty()) {
            order.put("couponCode", couponCode);
        }

        InMemoryDataStore.ORDERS.put(orderId, order);

        return Map.of(
            "orderId", orderId,
            "orderNumber", orderNumber,
            "status", "PENDING",
            "itemsTotal", itemsTotal,
            "discountAmount", discountAmount,
            "finalAmount", finalAmount,
            "items", orderItems
        );
    }

    /**
     * 주문 조회
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public Map<String, Object> getOrder(@PathVariable Long orderId) {
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            return Map.of("error", "주문을 찾을 수 없습니다");
        }
        return order;
    }

    /**
     * 주문 목록 조회
     * GET /api/orders
     */
    @GetMapping
    public Map<String, Object> getOrders(@RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> userOrders = new ArrayList<>();

        for (Map<String, Object> order : InMemoryDataStore.ORDERS.values()) {
            if (userId.equals(order.get("userId"))) {
                userOrders.add(order);
            }
        }

        // 최신순 정렬
        userOrders.sort((o1, o2) -> {
            String t1 = (String) o1.get("createdAt");
            String t2 = (String) o2.get("createdAt");
            return t2.compareTo(t1);
        });

        return Map.of("orders", userOrders, "total", userOrders.size());
    }

    /**
     * 주문 취소
     * POST /api/orders/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    public Map<String, Object> cancelOrder(@PathVariable Long orderId) {
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            return Map.of("error", "주문을 찾을 수 없습니다");
        }

        String status = (String) order.get("status");
        if ("CANCELLED".equals(status)) {
            return Map.of("error", "이미 취소된 주문입니다");
        }
        if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
            return Map.of("error", "결제 완료된 주문은 취소할 수 없습니다");
        }

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

        order.put("status", "CANCELLED");
        order.put("cancelledAt", LocalDateTime.now().toString());
        order.put("cancelReason", "사용자 요청");

        return Map.of(
            "orderId", orderId,
            "status", "CANCELLED",
            "message", "주문이 취소되었습니다"
        );
    }

    /**
     * 재고 예약 롤백 (주문 생성 실패 시)
     */
    private void rollbackInventoryReservation(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();

            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
            if (inventory != null) {
                int reserved = (int) inventory.get("reservedStock");
                inventory.put("reservedStock", Math.max(0, reserved - quantity));
            }
        }
    }
}
