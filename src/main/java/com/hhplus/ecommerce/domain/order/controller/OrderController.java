package com.hhplus.ecommerce.domain.order.controller;

import com.hhplus.ecommerce.domain.cart.exception.CartErrorCode;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.product.exception.ProductErrorCode;
import com.hhplus.ecommerce.global.common.dto.ApiResponse;
import com.hhplus.ecommerce.global.common.dto.PageMeta;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        List<Integer> cartItemIds = (List<Integer>) request.get("cartItemIds");
        String deliveryAddress = (String) request.get("deliveryAddress");
        String deliveryMemo = (String) request.getOrDefault("deliveryMemo", "");
        Long couponId = request.get("couponId") != null ? ((Number) request.get("couponId")).longValue() : null;

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        // 0. 장바구니 조회
        Map<String, Object> cart = InMemoryDataStore.CARTS.get(userId);
        if (cart == null) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
        }

        // 0-1. 장바구니 아이템 조회
        Long cartId = (Long) cart.get("cartId");
        List<Map<String, Object>> cartItems = InMemoryDataStore.CART_ITEMS.get(cartId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException(CartErrorCode.EMPTY_CART);
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
                throw new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND,
                    Map.of("cartItemId", cartItemId));
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
                throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
            }

            // 재고 확인
            Map<String, Object> inventory = InMemoryDataStore.INVENTORY.get(productId);
            int stock = (int) inventory.get("stock");
            int reserved = (int) inventory.get("reservedStock");

            if (stock - reserved < quantity) {
                throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK,
                    Map.of(
                        "productId", productId,
                        "productName", product.get("name"),
                        "requestedQuantity", quantity,
                        "availableStock", stock - reserved
                    ));
            }

            // 재고 예약
            inventory.put("reservedStock", reserved + quantity);

            // 주문 항목 생성
            long unitPrice = (Long) product.get("price");
            long subtotal = unitPrice * quantity;

            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("orderItemId", InMemoryDataStore.nextOrderItemId());
            orderItem.put("productId", productId);
            orderItem.put("productName", product.get("name"));
            orderItem.put("quantity", quantity);
            orderItem.put("price", unitPrice);
            orderItem.put("subtotal", subtotal);
            orderItems.add(orderItem);

            itemsTotal += subtotal;
        }

        // 2. 쿠폰 할인 계산
        long discountAmount = 0;
        Map<String, Object> appliedCoupon = null;
        if (couponId != null) {
            // 사용자가 해당 쿠폰을 발급받았는지 확인
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.get(userId);
            Map<String, Object> userCoupon = null;
            if (userCoupons != null) {
                for (Map<String, Object> uc : userCoupons) {
                    if (couponId.equals(uc.get("userCouponId")) && !((Boolean) uc.getOrDefault("isUsed", false))) {
                        userCoupon = uc;
                        break;
                    }
                }
            }

            if (userCoupon == null) {
                rollbackInventoryReservation(selectedItems);
                throw new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
            }

            Long couponMasterId = (Long) userCoupon.get("couponId");
            Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(couponMasterId);
            if (coupon == null) {
                rollbackInventoryReservation(selectedItems);
                throw new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
            }

            if (!"ACTIVE".equals(coupon.get("status"))) {
                rollbackInventoryReservation(selectedItems);
                throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
            }

            // 최소 주문 금액 확인
            long minOrderAmount = (Long) coupon.get("minOrderAmount");
            if (itemsTotal < minOrderAmount) {
                rollbackInventoryReservation(selectedItems);
                throw new BusinessException(CouponErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET,
                    Map.of(
                        "couponId", couponId,
                        "minOrderAmount", minOrderAmount,
                        "currentAmount", itemsTotal
                    ));
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

            // 적용된 쿠폰 정보 저장
            appliedCoupon = new HashMap<>();
            appliedCoupon.put("couponId", couponMasterId);
            appliedCoupon.put("name", coupon.get("name"));
            appliedCoupon.put("discountAmount", discountAmount);

            // userCoupon에 orderId 저장 (주문 생성 후 업데이트 예정)
            userCoupon.put("orderId", null);
        }

        long finalAmount = itemsTotal - discountAmount;

        // 3. 주문 생성
        Long orderId = InMemoryDataStore.nextOrderId();
        String orderNumber = InMemoryDataStore.generateOrderNumber();

        Map<String, Object> pricing = new HashMap<>();
        pricing.put("itemsTotal", itemsTotal);
        pricing.put("discountAmount", discountAmount);
        pricing.put("finalAmount", finalAmount);

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("userId", userId);
        order.put("orderNumber", orderNumber);
        order.put("status", "PENDING");
        order.put("pricing", pricing);
        order.put("items", orderItems);
        order.put("deliveryAddress", deliveryAddress);
        order.put("deliveryMemo", deliveryMemo);
        order.put("createdAt", LocalDateTime.now().toString());
        order.put("expiresAt", LocalDateTime.now().plusMinutes(15).toString());

        if (appliedCoupon != null) {
            order.put("coupon", appliedCoupon);
        }

        InMemoryDataStore.ORDERS.put(orderId, order);
        InMemoryDataStore.ORDER_ITEMS.put(orderId, orderItems);

        // 4. 장바구니에서 주문한 아이템 제거
        for (Integer cartItemId : cartItemIds) {
            cartItems.removeIf(item -> cartItemId.equals(((Number) item.get("cartItemId")).intValue()));
        }

        return ApiResponse.of(order);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> getOrder(@PathVariable Long orderId) {
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return ApiResponse.of(order);
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getOrders(
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

        // OrderSummary 형식으로 변환
        List<Map<String, Object>> orderSummaries = new ArrayList<>();
        for (Map<String, Object> order : pagedOrders) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            @SuppressWarnings("unchecked")
            Map<String, Object> pricing = (Map<String, Object>) order.get("pricing");

            Map<String, Object> summary = new HashMap<>();
            summary.put("orderId", order.get("orderId"));
            summary.put("orderNumber", order.get("orderNumber"));
            summary.put("status", order.get("status"));
            summary.put("itemCount", items != null ? items.size() : 0);
            summary.put("totalAmount", pricing != null ? pricing.get("finalAmount") : 0L);
            summary.put("createdAt", order.get("createdAt"));

            orderSummaries.add(summary);
        }

        PageMeta pageMeta = new PageMeta(page, size, totalElements, totalPages);
        return ApiResponse.of(orderSummaries, pageMeta);
    }

    @PatchMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> updateOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> request
    ) {
        String requestedStatus = (String) request.get("status");
        if (!"CANCELLED".equals(requestedStatus)) {
            throw new BusinessException(OrderErrorCode.INVALID_REQUEST);
        }

        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        String status = (String) order.get("status");
        if ("CANCELLED".equals(status)) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PAID,
                Map.of(
                    "orderId", orderId,
                    "status", status,
                    "paidAt", order.getOrDefault("paidAt", "")
                ));
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

        String reason = (String) request.getOrDefault("cancelReason", "사용자 요청");
        String cancelledAt = LocalDateTime.now().toString();

        order.put("status", "CANCELLED");
        order.put("cancelledAt", cancelledAt);
        order.put("cancelReason", reason);

        // 응답은 필수 필드만 반환
        return ApiResponse.of(Map.of(
            "orderId", orderId,
            "status", "CANCELLED",
            "cancelledAt", cancelledAt,
            "cancelReason", reason
        ));
    }

    @GetMapping("/{orderId}/payment")
    public ApiResponse<Map<String, Object>> getOrderPayment(@PathVariable Long orderId) {
        // 주문 확인
        Map<String, Object> order = InMemoryDataStore.ORDERS.get(orderId);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        // 결제 정보 조회
        for (Map<String, Object> payment : InMemoryDataStore.PAYMENTS.values()) {
            if (orderId.equals(payment.get("orderId"))) {
                return ApiResponse.of(payment);
            }
        }

        throw new BusinessException(com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

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
