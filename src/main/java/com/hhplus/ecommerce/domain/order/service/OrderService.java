package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.service.CartService;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.dto.*;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderItem;
import com.hhplus.ecommerce.domain.order.repository.OrderItemRepository;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.common.dto.PagedResult;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final CouponService couponService;

    public Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    return new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
                });
    }

    public Order requireOrderOwnedByUser(Long userId, Long orderId) {
        Order order = getOrderEntity(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return order;
    }

    public OrderResponse createOrder(Long userId, List<Long> cartItemIds, Long userCouponId,
                                     String deliveryAddress, String deliveryMemo) {
        List<CartItem> cartItems = getValidCartItems(userId, cartItemIds);
        Map<Long, Product> productMap = getProductsForOrder(cartItems);
        List<OrderItem> orderItems = createOrderItemsWithStockReservation(cartItems, productMap);

        long itemsTotal = calculateItemsTotal(orderItems);
        long discountAmount = 0L;
        Coupon coupon = null;

        if (userCouponId != null) {
            UserCoupon userCoupon = validateAndGetUserCoupon(userId, userCouponId);
            coupon = couponService.getCouponEntity(userCoupon.getCouponId());
            discountAmount = coupon.calculateDiscount(itemsTotal);
        }

        Order order = buildOrder(userId, orderItems, itemsTotal, discountAmount, userCouponId, deliveryAddress, deliveryMemo);
        Order savedOrder = orderRepository.save(order);

        saveOrderItems(savedOrder.getId(), orderItems);

        if (userCouponId != null) {
            couponService.useCoupon(userCouponId, savedOrder.getId());
        }

        cartService.removeCartItems(cartItemIds);

        return toOrderResponse(savedOrder, coupon, discountAmount);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = requireOrderOwnedByUser(userId, orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        Order orderWithItems = enrichOrderWithItems(order, items);

        Coupon coupon = null;
        if (orderWithItems.getUserCouponId() != null) {
            UserCoupon userCoupon = couponService.getUserCouponEntity(orderWithItems.getUserCouponId());
            coupon = couponService.getCouponEntity(userCoupon.getCouponId());
        }

        return toOrderResponse(orderWithItems, coupon, orderWithItems.getDiscountAmount());
    }

    public PagedResult<OrderSummaryResponse> getUserOrders(Long userId, int page, int size) {
        List<Order> allOrders = orderRepository.findByUserId(userId);
        List<OrderSummaryResponse> responses = allOrders.stream()
                .map(this::toOrderSummaryResponse)
                .toList();
        return PagedResult.of(responses, page, size);
    }

    public CancelOrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        requireOrderOwnedByUser(userId, orderId);
        return cancelOrderInternal(orderId, reason);
    }

    public CancelOrderResponse cancelOrder(Long orderId, String reason) {
        return cancelOrderInternal(orderId, reason);
    }

    private CancelOrderResponse cancelOrderInternal(Long orderId, String reason) {
        Order order = getOrderEntity(orderId);

        if (!order.isCancellable()) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        order.cancel(reason);
        orderRepository.save(order);

        releaseStockReservations(orderId);

        if (order.getUserCouponId() != null) {
            restoreCoupon(order.getUserCouponId());
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
        Order order = getOrderEntity(orderId);

        order.markAsPaid();
        orderRepository.save(order);

        confirmStockReservations(orderId);
    }

    private OrderResponse toOrderResponse(Order order, Coupon coupon, Long discountAmount) {
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

        OrderCouponResponse couponResponse = null;
        if (coupon != null && discountAmount != null && discountAmount > 0) {
            couponResponse = new OrderCouponResponse(
                    coupon.getId(),
                    coupon.getName(),
                    discountAmount
            );
        }

        return OrderResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus().name(),
                itemResponses,
                pricing,
                couponResponse,
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

    private UserCoupon validateAndGetUserCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = couponService.getUserCouponEntity(userCouponId);

        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.INVALID_COUPON_OWNER);
        }

        if (!userCoupon.isUsable()) {
            throw new BusinessException(OrderErrorCode.COUPON_NOT_USABLE);
        }

        return userCoupon;
    }

    private void restoreCoupon(Long userCouponId) {
        try {
            couponService.cancelCouponUse(userCouponId);
        } catch (Exception e) {
            // 쿠폰 복구 실패는 로깅만 하고 주문 취소는 계속 진행
            // 실제 운영 환경에서는 로깅 시스템을 사용해야 함
        }
    }

    private List<CartItem> getValidCartItems(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = cartService.getCartItemsByIds(userId, cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
        }
        return cartItems;
    }

    private Map<Long, Product> getProductsForOrder(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();
        return productService.getProductsAsMap(productIds);
    }

    private List<OrderItem> createOrderItemsWithStockReservation(List<CartItem> cartItems, Map<Long, Product> productMap) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
            }

            productService.reserveStock(product.getId(), cartItem.getQuantity());

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

        return orderItems;
    }

    private long calculateItemsTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    private Order buildOrder(Long userId, List<OrderItem> orderItems, long itemsTotal, long discountAmount,
                             Long userCouponId, String deliveryAddress, String deliveryMemo) {
        Long orderId = orderRepository.generateNextId();
        String orderNumber = orderRepository.generateOrderNumber();

        return Order.create(orderId, userId, orderNumber, orderItems,
                itemsTotal, discountAmount, userCouponId, deliveryAddress, deliveryMemo);
    }

    private void saveOrderItems(Long orderId, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            OrderItem itemWithOrderId = item.withOrderId(orderId);
            orderItemRepository.save(itemWithOrderId);
        }
    }

    private Order enrichOrderWithItems(Order order, List<OrderItem> items) {
        return order.withItems(items);
    }

    private void releaseStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.releaseReservation(item.getProductId(), item.getQuantity());
        }
    }

    private void confirmStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.confirmReservation(item.getProductId(), item.getQuantity());
        }
    }
}
