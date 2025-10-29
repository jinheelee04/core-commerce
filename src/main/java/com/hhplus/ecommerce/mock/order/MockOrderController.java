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
        List<Integer> cartItemIds = (List<Integer>) request.get("cartItemIds");
        String deliveryAddress = (String) request.get("deliveryAddress");
        String deliveryMemo = (String) request.getOrDefault("deliveryMemo", "");
        Long couponId = request.get("couponId") != null ? ((Number) request.get("couponId")).longValue() : null;

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("error", "주문 상품이 없습니다");
        }

        // 0. 장바구니 아이템 조회
        List<Map<String, Object>> cartItems = InMemoryDataStore.CARTS.get(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            return Map.of("error", "장바구니가 비어있습니다");
        }

        List<Map<String, Object>> selectedItems = new ArrayList<>();
        for (Integer cartItemId : cartItemIds) {
            boolean found = false;
            for (Map<String, Object> cartItem : cartItems) {
                if (cartItemId.equals(((Number) cartItem.get("cartItemId")).intValue())) {
                    selectedItems.add(cartItem);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return Map.of("error", "장바구니 아이템을 찾을 수 없습니다", "cartItemId", cartItemId);
            }
        }

        // 1. 재고 확인 및 예약
        List<Map<String, Object>> orderItems = new ArrayList<>();
        long itemsTotal = 0;

        for (Map<String, Object> item : selectedItems) {
            Long productId = (Long) item.get("productId");
            int quantity = (int) item.get("quantity");

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
        String couponCode = null;
        if (couponId != null) {
            // 사용자가 해당 쿠폰을 발급받았는지 확인
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.get(userId);
            Map<String, Object> userCoupon = null;
            if (userCoupons != null) {
                for (Map<String, Object> uc : userCoupons) {
                    if (couponId.equals(uc.get("userCouponId")) && !((Boolean) uc.getOrDefault("isUsed", false))) {
                        userCoupon = uc;
                        couponCode = (String) uc.get("code");
                        break;
                    }
                }
            }

            if (userCoupon == null) {
                rollbackInventoryReservation(selectedItems);
                return Map.of("error", "발급받지 않은 쿠폰이거나 이미 사용된 쿠폰입니다");
            }

            Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(couponCode);
            if (coupon == null) {
                rollbackInventoryReservation(selectedItems);
                return Map.of("error", "유효하지 않은 쿠폰입니다");
            }

            if (!"ACTIVE".equals(coupon.get("status"))) {
                rollbackInventoryReservation(selectedItems);
                return Map.of("error", "사용할 수 없는 쿠폰입니다");
            }

            // 최소 주문 금액 확인
            long minOrderAmount = (Long) coupon.get("minOrderAmount");
            if (itemsTotal < minOrderAmount) {
                rollbackInventoryReservation(selectedItems);
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

        // 4. 장바구니에서 주문한 아이템 제거
        for (Integer cartItemId : cartItemIds) {
            cartItems.removeIf(item -> cartItemId.equals(((Number) item.get("cartItemId")).intValue()));
        }

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
    public Map<String, Object> getOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startsAt,
            @RequestParam(required = false) String endsAt,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        List<Map<String, Object>> userOrders = new ArrayList<>();

        for (Map<String, Object> order : InMemoryDataStore.ORDERS.values()) {
            if (!userId.equals(order.get("userId"))) {
                continue;
            }

            // 상태 필터
            if (status != null && !status.equals(order.get("status"))) {
                continue;
            }

            // 날짜 필터
            String createdAt = (String) order.get("createdAt");
            if (startsAt != null && createdAt.compareTo(startsAt) < 0) {
                continue;
            }
            if (endsAt != null && createdAt.compareTo(endsAt) > 0) {
                continue;
            }

            userOrders.add(order);
        }

        // 최신순 정렬
        userOrders.sort((o1, o2) -> {
            String t1 = (String) o1.get("createdAt");
            String t2 = (String) o2.get("createdAt");
            return t2.compareTo(t1);
        });

        // 페이징 처리
        int totalElements = userOrders.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Map<String, Object>> pagedOrders = fromIndex < totalElements
            ? userOrders.subList(fromIndex, toIndex)
            : new ArrayList<>();

        return Map.of(
            "data", pagedOrders,
            "meta", Map.of(
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
            )
        );
    }

    /**
     * 주문 취소
     * PATCH /api/orders/{orderId}
     */
    @PatchMapping("/{orderId}")
    public Map<String, Object> updateOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> request
    ) {
        String action = (String) request.get("action");
        if (!"cancel".equals(action)) {
            return Map.of("error", "지원하지 않는 작업입니다");
        }

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

        String reason = (String) request.getOrDefault("reason", "사용자 요청");
        order.put("status", "CANCELLED");
        order.put("cancelledAt", LocalDateTime.now().toString());
        order.put("cancelReason", reason);

        return Map.of(
            "data", order
        );
    }

    /**
     * 주문의 결제 정보 조회
     * GET /api/orders/{orderId}/payment
     */
    @GetMapping("/{orderId}/payment")
    public Map<String, Object> getOrderPayment(@PathVariable Long orderId) {
        // 주문 확인
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            return Map.of("error", "주문을 찾을 수 없습니다");
        }

        // 결제 정보 조회
        for (Map<String, Object> payment : InMemoryDataStore.PAYMENTS.values()) {
            if (orderId.equals(payment.get("orderId"))) {
                return Map.of("data", payment);
            }
        }

        return Map.of("error", "결제 정보를 찾을 수 없습니다");
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
