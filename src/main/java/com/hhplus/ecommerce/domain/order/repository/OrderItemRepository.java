package com.hhplus.ecommerce.domain.order.repository;

import com.hhplus.ecommerce.domain.order.model.OrderItem;

import java.util.List;
import java.util.Optional;

/**
 * 주문 아이템 Repository 인터페이스
 * 주문 아이템 데이터 접근을 추상화
 */
public interface OrderItemRepository {
    /**
     * 주문 아이템 저장
     */
    OrderItem save(OrderItem orderItem);

    /**
     * ID로 주문 아이템 조회
     */
    Optional<OrderItem> findById(Long id);

    /**
     * 주문 ID로 아이템 목록 조회
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 상품 ID로 주문 아이템 조회
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 모든 주문 아이템 조회
     */
    List<OrderItem> findAll();

    /**
     * 주문 아이템 삭제
     */
    void deleteById(Long id);

    /**
     * 다음 ID 생성
     */
    Long generateNextId();
}
