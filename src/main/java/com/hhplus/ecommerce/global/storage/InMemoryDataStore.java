package com.hhplus.ecommerce.global.storage;

import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.DiscountType;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-Memory 데이터 저장소 (Deprecated - JPA 사용으로 인해 더 이상 사용되지 않음)
 *
 * 이 파일은 이전 버전과의 호환성을 위해 유지됩니다.
 * 새로운 코드에서는 JPA Repository를 사용하세요.
 */
@Deprecated
public class InMemoryDataStore {

    // ID 시퀀스
    public static final AtomicLong productIdSequence = new AtomicLong(5);
    public static final AtomicLong inventoryIdSequence = new AtomicLong(5);

    // 데이터 저장소
    public static final Map<Long, Product> PRODUCTS = new ConcurrentHashMap<>();
    public static final Map<Long, Inventory> INVENTORY = new ConcurrentHashMap<>();
    public static final Map<Long, Coupon> COUPONS = new ConcurrentHashMap<>();

    /**
     * 테스트용 데이터 초기화 메서드
     */
    public static void clear() {
        PRODUCTS.clear();
        INVENTORY.clear();
        COUPONS.clear();
    }
}
