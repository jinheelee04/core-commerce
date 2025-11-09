package com.hhplus.ecommerce.domain.order.repository;

import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository 인터페이스
 * 주문 데이터 접근을 추상화
 */
public interface OrderRepository {
    /**
     * 주문 저장
     */
    Order save(Order order);

    /**
     * ID로 주문 조회
     */
    Optional<Order> findById(Long id);

    /**
     * 주문 번호로 주문 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 사용자 ID로 주문 목록 조회
     */
    List<Order> findByUserId(Long userId);

    /**
     * 상태별 주문 조회
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 만료된 주문 조회
     */
    List<Order> findExpiredOrders(LocalDateTime now);

    /**
     * 모든 주문 조회
     */
    List<Order> findAll();

    /**
     * 주문 삭제
     */
    void deleteById(Long id);

    /**
     * 다음 ID 생성
     */
    Long generateNextId();

    /**
     * 주문 번호 생성
     */
    String generateOrderNumber();
}
