package com.hhplus.ecommerce.domain.order.repository;

import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository In-Memory 구현
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    @Override
    public Order save(Order order) {
        InMemoryDataStore.ORDERS.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.ORDERS.get(id));
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return InMemoryDataStore.ORDERS.values().stream()
                .filter(order -> order.getOrderNumber().equals(orderNumber))
                .findFirst();
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return InMemoryDataStore.ORDERS.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return InMemoryDataStore.ORDERS.values().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    @Override
    public List<Order> findExpiredOrders(LocalDateTime now) {
        return InMemoryDataStore.ORDERS.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .filter(order -> order.getExpiresAt() != null && order.getExpiresAt().isBefore(now))
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(InMemoryDataStore.ORDERS.values());
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.ORDERS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.orderIdSequence.incrementAndGet();
    }

    @Override
    public String generateOrderNumber() {
        return InMemoryDataStore.generateOrderNumber();
    }
}
