package com.hhplus.ecommerce.global.storage;

import com.hhplus.ecommerce.domain.cart.model.Cart;
import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderItem;
import com.hhplus.ecommerce.domain.payment.model.Payment;
import com.hhplus.ecommerce.domain.product.model.Inventory;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-Memory 데이터 저장소
 * JPA 도입 전까지 사용하는 임시 데이터 저장소
 */
public class
InMemoryDataStore {

    // ID 시퀀스
    public static final AtomicLong productIdSequence = new AtomicLong(5);
    public static final AtomicLong inventoryIdSequence = new AtomicLong(5);
    public static final AtomicLong cartIdSequence = new AtomicLong(1);
    public static final AtomicLong cartItemIdSequence = new AtomicLong(1);
    public static final AtomicLong orderIdSequence = new AtomicLong(1);
    public static final AtomicLong orderItemIdSequence = new AtomicLong(1);
    public static final AtomicLong paymentIdSequence = new AtomicLong(1);
    public static final AtomicLong couponIdSequence = new AtomicLong(3);
    public static final AtomicLong userCouponIdSequence = new AtomicLong(1);

    // 데이터 저장소
    public static final Map<Long, Product> PRODUCTS = new ConcurrentHashMap<>();
    public static final Map<Long, Inventory> INVENTORY = new ConcurrentHashMap<>();
    public static final Map<Long, Cart> CARTS = new ConcurrentHashMap<>();
    public static final Map<Long, List<CartItem>> CART_ITEMS = new ConcurrentHashMap<>();
    public static final Map<Long, Order> ORDERS = new ConcurrentHashMap<>();
    public static final Map<Long, List<OrderItem>> ORDER_ITEMS = new ConcurrentHashMap<>();
    public static final Map<Long, Payment> PAYMENTS = new ConcurrentHashMap<>();
    public static final Map<Long, Coupon> COUPONS = new ConcurrentHashMap<>();
    public static final Map<Long, UserCoupon> USER_COUPONS = new ConcurrentHashMap<>();

    // 초기 데이터 로드
    static {
        LocalDateTime now = LocalDateTime.now();

        PRODUCTS.put(1L, Product.builder()
                .id(1L)
                .name("노트북")
                .description("고성능 업무용 노트북")
                .price(890000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("삼성")
                .imageUrl("https://cdn.example.com/products/1.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        PRODUCTS.put(2L, Product.builder()
                .id(2L)
                .name("키보드")
                .description("기계식 키보드")
                .price(120000L)
                .category(ProductCategory.PERIPHERAL)
                .brand("로지텍")
                .imageUrl("https://cdn.example.com/products/2.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        PRODUCTS.put(3L, Product.builder()
                .id(3L)
                .name("마우스")
                .description("무선 마우스")
                .price(45000L)
                .category(ProductCategory.PERIPHERAL)
                .brand("로지텍")
                .imageUrl("https://cdn.example.com/products/3.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        PRODUCTS.put(4L, Product.builder()
                .id(4L)
                .name("MacBook Pro")
                .description("Apple M3 chip, 16GB RAM")
                .price(2500000L)
                .category(ProductCategory.ELECTRONICS)
                .brand("Apple")
                .imageUrl("https://cdn.example.com/products/4.jpg")
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        INVENTORY.put(1L, Inventory.builder()
                .id(1L)
                .productId(1L)
                .stock(10)
                .reservedStock(0)
                .lowStockThreshold(5)
                .createdAt(now)
                .updatedAt(now)
                .build());

        INVENTORY.put(2L, Inventory.builder()
                .id(2L)
                .productId(2L)
                .stock(50)
                .reservedStock(0)
                .lowStockThreshold(12)
                .createdAt(now)
                .updatedAt(now)
                .build());

        INVENTORY.put(3L, Inventory.builder()
                .id(3L)
                .productId(3L)
                .stock(100)
                .reservedStock(0)
                .lowStockThreshold(15)
                .createdAt(now)
                .updatedAt(now)
                .build());

        INVENTORY.put(4L, Inventory.builder()
                .id(4L)
                .productId(4L)
                .stock(30)
                .reservedStock(0)
                .lowStockThreshold(10)
                .createdAt(now)
                .updatedAt(now)
                .build());

        COUPONS.put(1L, Coupon.builder()
                .id(1L)
                .code("WELCOME10")
                .name("신규 회원 10% 할인")
                .description("신규 회원을 위한 10% 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(100)
                .startsAt(now)
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        COUPONS.put(2L, Coupon.builder()
                .id(2L)
                .code("FIXED5000")
                .name("5000원 할인 쿠폰")
                .description("5000원 정액 할인")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(5000)
                .minOrderAmount(50000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(50)
                .remainingQuantity(50)
                .startsAt(now)
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    /**
     * 주문 번호 생성
     */
    public static String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(formatter);
        return String.format("ORD-%s-%05d", dateStr, orderIdSequence.get());
    }

    /**
     * 테스트용 데이터 초기화 메서드
     */
    public static void clear() {
        PRODUCTS.clear();
        INVENTORY.clear();
        CARTS.clear();
        CART_ITEMS.clear();
        ORDERS.clear();
        ORDER_ITEMS.clear();
        PAYMENTS.clear();
        COUPONS.clear();
        USER_COUPONS.clear();
    }
}
