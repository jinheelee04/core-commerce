package com.hhplus.ecommerce.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_histories")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "previous_status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus newStatus;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // 비즈니스 로직용 생성자
    public OrderStatusHistory(Order order, OrderStatus previousStatus, OrderStatus newStatus,
                              Long changedBy, String reason) {
        this.order = order;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = LocalDateTime.now();
        this.changedBy = changedBy;
        this.reason = reason;
    }

    // 시스템에 의한 상태 변경 (changedBy = null)
    public static OrderStatusHistory createSystemChange(Order order, OrderStatus previousStatus,
                                                        OrderStatus newStatus, String reason) {
        return new OrderStatusHistory(order, previousStatus, newStatus, null, reason);
    }
}