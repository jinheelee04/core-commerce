package com.hhplus.ecommerce.domain.order.repository;

import com.hhplus.ecommerce.domain.order.model.OrderItem;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 주문 아이템 Repository In-Memory 구현
 */
@Repository
public class InMemoryOrderItemRepository implements OrderItemRepository {

    @Override
    public OrderItem save(OrderItem orderItem) {
        List<OrderItem> items = InMemoryDataStore.ORDER_ITEMS
                .computeIfAbsent(orderItem.getOrderId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        // 기존 아이템 제거 후 업데이트된 아이템 추가 (update 지원)
        items.removeIf(item -> item.getId().equals(orderItem.getId()));
        items.add(orderItem);

        return orderItem;
    }

    @Override
    public Optional<OrderItem> findById(Long id) {
        return InMemoryDataStore.ORDER_ITEMS.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return InMemoryDataStore.ORDER_ITEMS.getOrDefault(orderId, List.of());
    }

    @Override
    public List<OrderItem> findByProductId(Long productId) {
        return InMemoryDataStore.ORDER_ITEMS.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getProductId().equals(productId))
                .toList();
    }

    @Override
    public List<OrderItem> findAll() {
        return InMemoryDataStore.ORDER_ITEMS.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.ORDER_ITEMS.values().forEach(list ->
                list.removeIf(item -> item.getId().equals(id))
        );
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.orderItemIdSequence.incrementAndGet();
    }
}
