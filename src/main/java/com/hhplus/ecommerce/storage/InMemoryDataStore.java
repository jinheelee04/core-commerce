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
    private static final AtomicLong cartIdSeq = new AtomicLong(1);
    private static final AtomicLong cartItemIdSeq = new AtomicLong(1);
    private static final AtomicLong paymentIdSeq = new AtomicLong(1);
    private static final AtomicLong userCouponIdSeq = new AtomicLong(1);
    private static final AtomicLong orderItemIdSeq = new AtomicLong(1);

    // ===== 상품 데이터 =====
    public static final Map<Long, Map<String, Object>> PRODUCTS = new ConcurrentHashMap<>();

    static {
        PRODUCTS.put(1L, new HashMap<>(Map.of(
                "productId", 1L,
                "name", "노트북",
                "description", "고성능 업무용 노트북",
                "price", 890000L,
                "category", "ELECTRONICS",
                "brand", "삼성",
                "imageUrl", "https://cdn.example.com/products/1.jpg",
                "status", "AVAILABLE",
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        PRODUCTS.put(2L, new HashMap<>(Map.of(
                "productId", 2L,
                "name", "키보드",
                "description", "기계식 키보드",
                "price", 120000L,
                "category", "PERIPHERAL",
                "brand", "로지텍",
                "imageUrl", "https://cdn.example.com/products/2.jpg",
                "status", "AVAILABLE",
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        PRODUCTS.put(3L, new HashMap<>(Map.of(
                "productId", 3L,
                "name", "마우스",
                "description", "무선 마우스",
                "price", 45000L,
                "category", "PERIPHERAL",
                "brand", "로지텍",
                "imageUrl", "https://cdn.example.com/products/3.jpg",
                "status", "AVAILABLE",
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        PRODUCTS.put(4L, new HashMap<>(Map.of(
                "productId", 4L,
                "name", "MacBook Pro",
                "description", "Apple M3 chip, 16GB RAM",
                "price", 2500000L,
                "category", "ELECTRONICS",
                "brand", "Apple",
                "imageUrl", "https://cdn.example.com/products/4.jpg",
                "status", "AVAILABLE",
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));
    }

    // ===== 재고 데이터 =====
    public static final Map<Long, Map<String, Object>> INVENTORY = new ConcurrentHashMap<>();

    static {
        INVENTORY.put(1L, new HashMap<>(Map.of(
                "inventoryId", 1L,
                "productId", 1L,
                "stock", 10,
                "reservedStock", 0,
                "lowStockThreshold", 5,
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        INVENTORY.put(2L, new HashMap<>(Map.of(
                "inventoryId", 2L,
                "productId", 2L,
                "stock", 50,
                "reservedStock", 0,
                "lowStockThreshold", 12,
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        INVENTORY.put(3L, new HashMap<>(Map.of(
                "inventoryId", 3L,
                "productId", 3L,
                "stock", 100,
                "reservedStock", 0,
                "lowStockThreshold", 15,
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));

        INVENTORY.put(4L, new HashMap<>(Map.of(
                "inventoryId", 4L,
                "productId", 4L,
                "stock", 30,
                "reservedStock", 0,
                "lowStockThreshold", 10,
                "createdAt", LocalDateTime.now().toString(),
                "updatedAt", LocalDateTime.now().toString()
        )));
    }

    // ===== 장바구니 데이터 (userId -> List<CartItem>) =====
    public static final Map<Long, Map<String, Object>> CARTS = new ConcurrentHashMap<>();
    public static final Map<Long, List<Map<String, Object>>> CART_ITEMS = new ConcurrentHashMap<>();

    // ===== 주문 데이터 =====
    public static final Map<Long, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();
    public static final Map<Long, List<Map<String, Object>>> ORDER_ITEMS = new ConcurrentHashMap<>();

    // ===== 결제 데이터 =====
    public static final Map<Long, Map<String, Object>> PAYMENTS = new ConcurrentHashMap<>();

    // ===== 쿠폰 데이터 =====
    public static final Map<Long, Map<String, Object>> COUPONS = new ConcurrentHashMap<>();

    static {
        COUPONS.put(1L, new HashMap<>(Map.ofEntries(
                Map.entry("couponId", 1L),
                Map.entry("code", "WELCOME10"),
                Map.entry("name", "신규 회원 10% 할인"),
                Map.entry("description", "신규 회원을 위한 10% 할인 쿠폰"),
                Map.entry("discountType", "PERCENTAGE"),
                Map.entry("discountValue", 10),
                Map.entry("minOrderAmount", 10000L),
                Map.entry("maxDiscountAmount", 5000L),
                Map.entry("totalQuantity", 100),
                Map.entry("remainingQuantity", 100),
                Map.entry("startsAt", LocalDateTime.now().toString()),
                Map.entry("endsAt", LocalDateTime.now().plusDays(30).toString()),
                Map.entry("status", "ACTIVE")
//                Map.entry("createdAt", LocalDateTime.now().toString()),
//                Map.entry("updatedAt", LocalDateTime.now().toString())
        )));

        COUPONS.put(2L, new HashMap<>(Map.ofEntries(
                Map.entry("couponId", 2L),
                Map.entry("code", "FIXED5000"),
                Map.entry("name", "5000원 할인 쿠폰"),
                Map.entry("description", "5000원 정액 할인"),
                Map.entry("discountType", "FIXED_AMOUNT"),
                Map.entry("discountValue", 5000),
                Map.entry("minOrderAmount", 50000L),
                Map.entry("maxDiscountAmount", 5000L),
                Map.entry("totalQuantity", 50),
                Map.entry("remainingQuantity", 50),
                Map.entry("startsAt", LocalDateTime.now().toString()),
                Map.entry("endsAt", LocalDateTime.now().plusDays(30).toString()),
                Map.entry("status", "ACTIVE")
//                Map.entry("createdAt", LocalDateTime.now().toString()),
//                Map.entry("updatedAt", LocalDateTime.now().toString())
        )));
    }

    // ===== 사용자 쿠폰 데이터 (userId -> List<UserCoupon>) =====
    public static final Map<Long, List<Map<String, Object>>> USER_COUPONS = new ConcurrentHashMap<>();

    // ===== ID 생성 메서드 =====
    public static Long nextOrderId() {
        return orderIdSeq.getAndIncrement();
    }

    public static Long nextCartId() {
        return cartIdSeq.getAndIncrement();
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

    public static Long nextOrderItemId() {
        return orderItemIdSeq.getAndIncrement();
    }

    // ===== 주문번호 생성 =====
    public static String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(formatter);
        return String.format("ORD-%s-%05d", dateStr, orderIdSeq.get());
    }
}
