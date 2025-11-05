package com.hhplus.ecommerce.domain.order.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class Order {
    private Long id;
    private Long userId;
    private String orderNumber;
    private OrderStatus status;
    private Long itemsTotal;
    private Long discountAmount;
    private Long finalAmount;
    private String deliveryAddress;
    private String deliveryMemo;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.PAID;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 중인 주문만 결제 처리할 수 있습니다: " + status);
        }
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 확정할 수 있습니다.");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (!isCancellable()) {
            throw new IllegalStateException("취소 불가능한 주문 상태입니다: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public int getTotalQuantity() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    public long calculateItemsTotal() {
        if (items == null || items.isEmpty()) return 0L;
        return items.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    public long calculateFinalAmount() {
        return itemsTotal - discountAmount;
    }

    public Order withItems(List<OrderItem> items) {
        return Order.builder()
                .id(this.id)
                .userId(this.userId)
                .orderNumber(this.orderNumber)
                .status(this.status)
                .itemsTotal(this.itemsTotal)
                .discountAmount(this.discountAmount)
                .finalAmount(this.finalAmount)
                .deliveryAddress(this.deliveryAddress)
                .deliveryMemo(this.deliveryMemo)
                .expiresAt(this.expiresAt)
                .paidAt(this.paidAt)
                .cancelledAt(this.cancelledAt)
                .cancelReason(this.cancelReason)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .items(items != null ? new ArrayList<>(items) : new ArrayList<>())
                .build();
    }

    public static Order create(Long id, Long userId, String orderNumber, List<OrderItem> orderItems,
                               long itemsTotal, long discountAmount, String deliveryAddress, String deliveryMemo) {
        LocalDateTime now = LocalDateTime.now();
        long finalAmount = itemsTotal - discountAmount;

        return Order.builder()
                .id(id)
                .userId(userId)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .itemsTotal(itemsTotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .deliveryAddress(deliveryAddress)
                .deliveryMemo(deliveryMemo)
                .expiresAt(now.plusMinutes(10))
                .createdAt(now)
                .updatedAt(now)
                .items(orderItems != null ? new ArrayList<>(orderItems) : new ArrayList<>())
                .build();
    }
}
