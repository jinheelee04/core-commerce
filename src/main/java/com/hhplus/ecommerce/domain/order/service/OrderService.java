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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;

    public Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("[Order] 주문 조회 실패 - orderId: {}", orderId);
                    return new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
                });
    }

    public Order requireOrderOwnedByUser(Long userId, Long orderId) {
        log.debug("[Order] 주문 소유권 검증 - userId: {}, orderId: {}", userId, orderId);

        Order order = getOrderEntity(orderId);

        if (!order.getUserId().equals(userId)) {
            log.warn("[Order] 주문 접근 권한 없음 - userId: {}, orderId: {}, actualUserId: {}",
                    userId, orderId, order.getUserId());
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        log.debug("[Order] 주문 소유권 검증 완료 - orderId: {}", orderId);
        return order;
    }

    public OrderResponse createOrder(Long userId, List<Long> cartItemIds, String deliveryAddress, String deliveryMemo) {
        log.info("[Order] 주문 생성 시작 - userId: {}, cartItemCount: {}", userId, cartItemIds.size());

        List<CartItem> cartItems = getValidCartItems(userId, cartItemIds);

        Map<Long, Product> productMap = getProductsForOrder(cartItems);

        List<OrderItem> orderItems = createOrderItemsWithStockReservation(cartItems, productMap);

        long itemsTotal = calculateItemsTotal(orderItems);
        long discountAmount = 0L;
        long finalAmount = itemsTotal - discountAmount;

        Order order = buildOrder(userId, orderItems, itemsTotal, discountAmount, finalAmount, deliveryAddress, deliveryMemo);
        Order savedOrder = orderRepository.save(order);
        log.debug("[Order] 주문 저장 완료 - orderId: {}, orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        saveOrderItems(savedOrder.getId(), orderItems);

        cartService.removeCartItems(cartItemIds);

        log.info("[Order] 주문 생성 완료 - orderId: {}, orderNumber: {}, finalAmount: {}",
                savedOrder.getId(), savedOrder.getOrderNumber(), finalAmount);

        return toOrderResponse(savedOrder);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        log.debug("[Order] 주문 조회 요청 - userId: {}, orderId: {}", userId, orderId);

        Order order = requireOrderOwnedByUser(userId, orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        Order orderWithItems = enrichOrderWithItems(order, items);
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
        log.info("[Order] 주문 취소 요청 (사용자) - userId: {}, orderId: {}, reason: {}", userId, orderId, reason);
        requireOrderOwnedByUser(userId, orderId);
        return cancelOrderInternal(orderId, reason);
    }

    public CancelOrderResponse cancelOrder(Long orderId, String reason) {
        log.info("[Order] 주문 취소 요청 (시스템) - orderId: {}, reason: {}", orderId, reason);
        return cancelOrderInternal(orderId, reason);
    }

    private CancelOrderResponse cancelOrderInternal(Long orderId, String reason) {
        Order order = getOrderEntity(orderId);

        if (!order.isCancellable()) {
            log.warn("[Order] 취소 불가능한 주문 - orderId: {}, status: {}", orderId, order.getStatus());
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        order.cancel(reason);
        orderRepository.save(order);
        log.debug("[Order] 주문 상태 CANCELLED로 변경 완료 - orderId: {}", orderId);

        releaseStockReservations(orderId);

        log.info("[Order] 주문 취소 완료 - orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());

        return CancelOrderResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getCancelReason(),
                order.getCancelledAt()
        );
    }

    public void completePayment(Long orderId) {
        log.info("[Order] 결제 완료 처리 시작 - orderId: {}", orderId);

        Order order = getOrderEntity(orderId);

        order.markAsPaid();
        orderRepository.save(order);
        log.debug("[Order] 주문 상태 PAID로 변경 완료 - orderId: {}", orderId);

        confirmStockReservations(orderId);

        log.info("[Order] 결제 완료 처리 완료 - orderId: {}, orderNumber: {}", orderId, order.getOrderNumber());
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

    // ========== Private Helper Methods ==========

    private List<CartItem> getValidCartItems(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = cartService.getCartItemsByIds(userId, cartItemIds);
        if (cartItems.isEmpty()) {
            log.warn("[Order] 유효한 장바구니 아이템 없음 - userId: {}, requestedIds: {}", userId, cartItemIds);
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
        }
        log.debug("[Order] 장바구니 아이템 조회 완료 - count: {}", cartItems.size());
        return cartItems;
    }

    private Map<Long, Product> getProductsForOrder(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();
        Map<Long, Product> productMap = productService.getProductsAsMap(productIds);
        log.debug("[Order] 상품 정보 조회 완료 - productCount: {}", productMap.size());
        return productMap;
    }

    private List<OrderItem> createOrderItemsWithStockReservation(List<CartItem> cartItems, Map<Long, Product> productMap) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                log.error("[Order] 상품 정보 없음 - productId: {}", cartItem.getProductId());
                throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
            }

            productService.reserveStock(product.getId(), cartItem.getQuantity());

            // 도메인 모델의 정적 팩토리 메서드 사용
            Long itemId = orderItemRepository.generateNextId();
            OrderItem orderItem = OrderItem.create(
                    itemId,
                    product.getId(),
                    product.getName(),
                    cartItem.getQuantity(),
                    product.getPrice()
            );
            orderItems.add(orderItem);
        }

        log.debug("[Order] 주문 아이템 생성 및 재고 예약 완료 - itemCount: {}", orderItems.size());
        return orderItems;
    }

    private long calculateItemsTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    private Order buildOrder(Long userId, List<OrderItem> orderItems, long itemsTotal, long discountAmount,
                             long finalAmount, String deliveryAddress, String deliveryMemo) {
        Long orderId = orderRepository.generateNextId();
        String orderNumber = orderRepository.generateOrderNumber();

        // 도메인 모델의 정적 팩토리 메서드 사용
        return Order.create(orderId, userId, orderNumber, orderItems,
                itemsTotal, discountAmount, deliveryAddress, deliveryMemo);
    }

    private void saveOrderItems(Long orderId, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            // 도메인 모델의 withOrderId 메서드 사용
            OrderItem itemWithOrderId = item.withOrderId(orderId);
            orderItemRepository.save(itemWithOrderId);
        }
        log.debug("[Order] 주문 아이템 저장 완료 - itemCount: {}", orderItems.size());
    }

    private Order enrichOrderWithItems(Order order, List<OrderItem> items) {
        // 도메인 모델의 withItems 메서드 사용
        return order.withItems(items);
    }

    private void releaseStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.releaseReservation(item.getProductId(), item.getQuantity());
        }
        log.debug("[Order] 재고 예약 해제 완료 - orderId: {}, itemCount: {}", orderId, items.size());
    }

    private void confirmStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.confirmReservation(item.getProductId(), item.getQuantity());
        }
        log.debug("[Order] 재고 예약 확정 완료 - orderId: {}, itemCount: {}", orderId, items.size());
    }
}
