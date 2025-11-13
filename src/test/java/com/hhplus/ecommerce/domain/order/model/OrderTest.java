package com.hhplus.ecommerce.domain.order.model;

import com.hhplus.ecommerce.domain.order.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order 도메인 모델 단위 테스트")
class OrderTest {

    @Nested
    @DisplayName("calculateItemsTotal 메서드는")
    class CalculateItemsTotalTest {

        @Test
        @DisplayName("주문 항목이 비어있으면 0을 반환한다")
        void shouldReturnZeroWhenItemsEmpty() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(0L)
                    .discountAmount(0L)
                    .finalAmount(0L)
                    .items(new ArrayList<>())
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateItemsTotal();

            // then
            assertThat(result).isEqualTo(0L);
        }

        @Test
        @DisplayName("주문 항목이 null이면 0을 반환한다")
        void shouldReturnZeroWhenItemsNull() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(0L)
                    .discountAmount(0L)
                    .finalAmount(0L)
                    .items(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateItemsTotal();

            // then
            assertThat(result).isEqualTo(0L);
        }

        @Test
        @DisplayName("단일 주문 항목의 금액을 정확히 계산한다")
        void shouldCalculateSingleItemTotal() {
            // given
            OrderItem item = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(100L)
                    .productName("테스트 상품")
                    .quantity(2)
                    .unitPrice(10000L)
                    .subtotal(20000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(20000L)
                    .discountAmount(0L)
                    .finalAmount(20000L)
                    .items(List.of(item))
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateItemsTotal();

            // then
            assertThat(result).isEqualTo(20000L);
        }

        @Test
        @DisplayName("여러 주문 항목의 금액 합계를 정확히 계산한다")
        void shouldCalculateMultipleItemsTotal() {
            // given
            OrderItem item1 = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(100L)
                    .productName("상품 1")
                    .quantity(2)
                    .unitPrice(10000L)
                    .subtotal(20000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            OrderItem item2 = OrderItem.builder()
                    .id(2L)
                    .orderId(1L)
                    .productId(101L)
                    .productName("상품 2")
                    .quantity(1)
                    .unitPrice(15000L)
                    .subtotal(15000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            OrderItem item3 = OrderItem.builder()
                    .id(3L)
                    .orderId(1L)
                    .productId(102L)
                    .productName("상품 3")
                    .quantity(3)
                    .unitPrice(5000L)
                    .subtotal(15000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(50000L)
                    .discountAmount(0L)
                    .finalAmount(50000L)
                    .items(List.of(item1, item2, item3))
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateItemsTotal();

            // then
            assertThat(result).isEqualTo(50000L);
            assertThat(result).isEqualTo(order.getItemsTotal());
        }

        @Test
        @DisplayName("저장된 subtotal 값을 사용하여 가격 변동에 영향받지 않는다")
        void shouldUseStoredSubtotalNotRecalculate() {
            // given
            OrderItem item = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(100L)
                    .productName("테스트 상품")
                    .quantity(2)
                    .unitPrice(15000L)
                    .subtotal(20000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(20000L)
                    .discountAmount(0L)
                    .finalAmount(20000L)
                    .items(List.of(item))
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateItemsTotal();

            // then
            assertThat(result).isEqualTo(20000L);
            assertThat(result).isNotEqualTo(item.calculateSubtotal()); // 30000
        }
    }

    @Nested
    @DisplayName("calculateFinalAmount 메서드는")
    class CalculateFinalAmountTest {

        @Test
        @DisplayName("할인이 없으면 상품 금액 그대로 반환한다")
        void shouldReturnItemsTotalWhenNoDiscount() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(50000L)
                    .discountAmount(0L)
                    .finalAmount(50000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateFinalAmount();

            // then
            assertThat(result).isEqualTo(50000L);
            assertThat(result).isEqualTo(order.getFinalAmount());
        }

        @Test
        @DisplayName("할인 금액을 차감한 최종 금액을 반환한다")
        void shouldReturnItemsTotalMinusDiscount() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(50000L)
                    .discountAmount(5000L)
                    .finalAmount(45000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateFinalAmount();

            // then
            assertThat(result).isEqualTo(45000L);
            assertThat(result).isEqualTo(order.getFinalAmount());
        }

        @Test
        @DisplayName("할인율 쿠폰 적용 시 정확한 금액을 계산한다")
        void shouldCalculateCorrectAmountWithPercentageDiscount() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(100000L)
                    .discountAmount(10000L)
                    .finalAmount(90000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateFinalAmount();

            // then
            assertThat(result).isEqualTo(90000L);
        }

        @Test
        @DisplayName("정액 할인 쿠폰 적용 시 정확한 금액을 계산한다")
        void shouldCalculateCorrectAmountWithFixedDiscount() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(30000L)
                    .discountAmount(5000L)
                    .finalAmount(25000L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            long result = order.calculateFinalAmount();

            // then
            assertThat(result).isEqualTo(25000L);
        }
    }

    @Nested
    @DisplayName("getTotalQuantity 메서드는")
    class GetTotalQuantityTest {

        @Test
        @DisplayName("주문 항목이 비어있으면 0을 반환한다")
        void shouldReturnZeroWhenItemsEmpty() {
            // given
            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(0L)
                    .discountAmount(0L)
                    .finalAmount(0L)
                    .items(new ArrayList<>())
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            int result = order.getTotalQuantity();

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("여러 주문 항목의 수량 합계를 정확히 계산한다")
        void shouldCalculateTotalQuantity() {
            // given
            OrderItem item1 = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(100L)
                    .quantity(2)
                    .unitPrice(10000L)
                    .subtotal(20000L)
                    .build();

            OrderItem item2 = OrderItem.builder()
                    .id(2L)
                    .orderId(1L)
                    .productId(101L)
                    .quantity(3)
                    .unitPrice(15000L)
                    .subtotal(45000L)
                    .build();

            OrderItem item3 = OrderItem.builder()
                    .id(3L)
                    .orderId(1L)
                    .productId(102L)
                    .quantity(1)
                    .unitPrice(5000L)
                    .subtotal(5000L)
                    .build();

            Order order = Order.builder()
                    .id(1L)
                    .userId(1L)
                    .orderNumber("ORD-20250101-00001")
                    .status(OrderStatus.PENDING)
                    .itemsTotal(70000L)
                    .discountAmount(0L)
                    .finalAmount(70000L)
                    .items(List.of(item1, item2, item3))
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            int result = order.getTotalQuantity();

            // then
            assertThat(result).isEqualTo(6);
        }
    }
}
