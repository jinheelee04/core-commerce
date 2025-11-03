package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.service.CartService;
import com.hhplus.ecommerce.domain.order.dto.*;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import com.hhplus.ecommerce.domain.order.model.OrderItem;
import com.hhplus.ecommerce.domain.order.repository.OrderItemRepository;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.common.dto.PagedResult;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;

    public OrderResponse createOrder(Long userId, List<Long> cartItemIds, String deliveryAddress, String deliveryMemo) {
        // 장바구니에서 선택한 항목만 조회
        List<CartItem> cartItems = cartService.getCartItemsByIds(userId, cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
        }

        // 상품 정보 일괄 조회
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();
        Map<Long, Product> productMap = productService.getProductsAsMap(productIds);

        List<OrderItem> orderItems = new ArrayList<>();
        long itemsTotal = 0L;

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
            }

            // 재고 예약
            productService.reserveStock(product.getId(), cartItem.getQuantity());

            long subtotal = product.getPrice() * cartItem.getQuantity();
            itemsTotal += subtotal;

            OrderItem orderItem = OrderItem.builder()
                    .id(orderItemRepository.generateNextId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            orderItems.add(orderItem);
        }

        // 쿠폰 미사용 (현재 단계에서는 할인 없음)
        long discountAmount = 0L;
        long finalAmount = itemsTotal;

        Order order = Order.builder()
                .id(orderRepository.generateNextId())
                .userId(userId)
                .orderNumber(orderRepository.generateOrderNumber())
                .status(OrderStatus.PENDING)
                .itemsTotal(itemsTotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .deliveryAddress(deliveryAddress)
                .deliveryMemo(deliveryMemo)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            OrderItem itemWithOrderId = OrderItem.builder()
                    .id(item.getId())
                    .orderId(savedOrder.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .createdAt(item.getCreatedAt())
                    .updatedAt(item.getUpdatedAt())
                    .build();
            orderItemRepository.save(itemWithOrderId);
        }

        // 선택한 장바구니 항목만 삭제
        cartService.removeCartItems(cartItemIds);

        return toOrderResponse(savedOrder);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Order orderWithItems = Order.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .itemsTotal(order.getItemsTotal())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryMemo(order.getDeliveryMemo())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .cancelledAt(order.getCancelledAt())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();

        return toOrderResponse(orderWithItems);
    }

    public PagedResult<OrderSummaryResponse> getUserOrders(Long userId, int page, int size) {
        List<Order> allOrders = orderRepository.findByUserId(userId);
        List<OrderSummaryResponse> responses = allOrders.stream()
                .map(this::toOrderSummaryResponse)
                .toList();
        return PagedResult.of(responses, page, size);
    }

    public CancelOrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return cancelOrderInternal(orderId, reason);
    }

    /**
     * 주문 취소 (내부 호출용 - 소유권 검증 없음)
     * PaymentService 등 다른 서비스에서 호출할 때 사용
     */
    public CancelOrderResponse cancelOrder(Long orderId, String reason) {
        return cancelOrderInternal(orderId, reason);
    }

    private CancelOrderResponse cancelOrderInternal(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.isCancellable()) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        order.cancel(reason);
        orderRepository.save(order);

        // 재고 예약 해제
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.releaseReservation(item.getProductId(), item.getQuantity());
        }

        return CancelOrderResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getCancelReason(),
                order.getCancelledAt()
        );
    }

    public void completePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        order.markAsPaid();
        orderRepository.save(order);

        // 재고 예약 확정
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.confirmReservation(item.getProductId(), item.getQuantity());
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.of(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        OrderPricingResponse pricing = OrderPricingResponse.of(
                order.getItemsTotal(),
                order.getDiscountAmount(),
                order.getFinalAmount()
        );

        // 쿠폰 정보는 주문 애그리거트에 포함되지 않음 (필요시 별도 조회)
        OrderCouponResponse coupon = null;

        return OrderResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus().name(),
                itemResponses,
                pricing,
                coupon,
                order.getDeliveryAddress(),
                order.getDeliveryMemo(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCancelReason(),
                order.getCreatedAt()
        );
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        return OrderSummaryResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getFinalAmount(),
                order.getTotalQuantity(),
                order.getCreatedAt()
        );
    }
}
