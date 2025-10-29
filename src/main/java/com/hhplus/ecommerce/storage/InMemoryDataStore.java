package com.hhplus.ecommerce.storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 기반 데이터 저장소
 * 실제 프로젝트에서는 DB(JPA)를 사용하지만, 과제용으로 간단히 메모리에 저장
 */
public class InMemoryDataStore {

    // ID 생성기
    private static final AtomicLong orderIdSeq = new AtomicLong(1);
    private static final AtomicLong cartItemIdSeq = new AtomicLong(1);
    private static final AtomicLong paymentIdSeq = new AtomicLong(1);
    private static final AtomicLong userCouponIdSeq = new AtomicLong(1);

    // ===== 상품 데이터 =====
    public static final Map<Long, Map<String, Object>> PRODUCTS = new ConcurrentHashMap<>();

    static {
        PRODUCTS.put(1L, new HashMap<>(Map.of(
            "productId", 1L,
            "name", "노트북",
            "description", "고성능 업무용 노트북",
            "price", 890000L,
            "category", "전자제품",
            "brand", "삼성",
            "status", "AVAILABLE",
            "createdAt", LocalDateTime.now().toString()
        )));

        PRODUCTS.put(2L, new HashMap<>(Map.of(
            "productId", 2L,
            "name", "키보드",
            "description", "기계식 키보드",
            "price", 120000L,
            "category", "주변기기",
            "brand", "로지텍",
            "status", "AVAILABLE",
            "createdAt", LocalDateTime.now().toString()
        )));

        PRODUCTS.put(3L, new HashMap<>(Map.of(
            "productId", 3L,
            "name", "마우스",
            "description", "무선 마우스",
            "price", 45000L,
            "category", "주변기기",
            "brand", "로지텍",
            "status", "AVAILABLE",
            "createdAt", LocalDateTime.now().toString()
        )));
    }

    // ===== 재고 데이터 =====
    public static final Map<Long, Map<String, Object>> INVENTORY = new ConcurrentHashMap<>();

    static {
        INVENTORY.put(1L, new HashMap<>(Map.of(
            "productId", 1L,
            "stock", 10,
            "reservedStock", 0
        )));

        INVENTORY.put(2L, new HashMap<>(Map.of(
            "productId", 2L,
            "stock", 50,
            "reservedStock", 0
        )));

        INVENTORY.put(3L, new HashMap<>(Map.of(
            "productId", 3L,
            "stock", 100,
            "reservedStock", 0
        )));
    }

    // ===== 장바구니 데이터 (userId -> List<CartItem>) =====
    public static final Map<Long, List<Map<String, Object>>> CARTS = new ConcurrentHashMap<>();

    // ===== 주문 데이터 =====
    public static final Map<Long, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();

    // ===== 결제 데이터 =====
    public static final Map<Long, Map<String, Object>> PAYMENTS = new ConcurrentHashMap<>();

    // ===== 쿠폰 데이터 =====
    public static final Map<String, Map<String, Object>> COUPONS = new ConcurrentHashMap<>();

    static {
        COUPONS.put("WELCOME10", new HashMap<>(Map.of(
            "code", "WELCOME10",
            "name", "신규 회원 10% 할인",
            "description", "신규 회원을 위한 10% 할인 쿠폰",
            "discountType", "PERCENTAGE",
            "discountValue", 10,
            "minOrderAmount", 10000L,
            "maxDiscountAmount", 5000L,
            "totalQuantity", 100,
            "remainingQuantity", 100,
            "status", "ACTIVE"
        )));

        COUPONS.put("FIXED5000", new HashMap<>(Map.of(
            "code", "FIXED5000",
            "name", "5000원 할인 쿠폰",
            "description", "5000원 정액 할인",
            "discountType", "FIXED_AMOUNT",
            "discountValue", 5000,
            "minOrderAmount", 50000L,
            "maxDiscountAmount", 5000L,
            "totalQuantity", 50,
            "remainingQuantity", 50,
            "status", "ACTIVE"
        )));
    }

    // ===== 사용자 쿠폰 데이터 (userId -> List<UserCoupon>) =====
    public static final Map<Long, List<Map<String, Object>>> USER_COUPONS = new ConcurrentHashMap<>();

    // ===== ID 생성 메서드 =====
    public static Long nextOrderId() {
        return orderIdSeq.getAndIncrement();
    }

    public static Long nextCartItemId() {
        return cartItemIdSeq.getAndIncrement();
    }

    public static Long nextPaymentId() {
        return paymentIdSeq.getAndIncrement();
    }

    public static Long nextUserCouponId() {
        return userCouponIdSeq.getAndIncrement();
    }

    // ===== 주문번호 생성 =====
    public static String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(formatter);
        return String.format("ORD-%s-%05d", dateStr, orderIdSeq.get());
    }
}
