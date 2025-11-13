package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.cart.entity.CartItem;
import com.hhplus.ecommerce.domain.cart.service.CartService;
import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.dto.*;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.entity.OrderItem;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.repository.OrderItemRepository;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.payment.event.PaymentCompletedEvent;
import com.hhplus.ecommerce.domain.payment.event.PaymentFailedEvent;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.domain.user.repository.UserRepository;
import com.hhplus.ecommerce.global.dto.PagedResult;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final CouponService couponService;

    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Order requireOrderOwnedByUser(Long userId, Long orderId) {
        Order order = orderRepository.findByIdWithUser(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        return order;
    }

    @Transactional
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
            coupon = couponService.findCouponById(userCoupon.getCoupon().getId());
            discountAmount = coupon.calculateDiscountAmount(itemsTotal);
        }

        Order order = buildOrder(userId, orderItems, itemsTotal, discountAmount, userCouponId, deliveryAddress, deliveryMemo);
        Order savedOrder = orderRepository.save(order);

        saveOrderItems(savedOrder, orderItems);

        if (userCouponId != null) {
            couponService.reserveCoupon(userCouponId, savedOrder.getId());
        }

        cartService.removeCartItems(cartItemIds);

        List<OrderItem> savedOrderItems = orderItemRepository.findByOrderId(savedOrder.getId());

        return toOrderResponse(savedOrder, savedOrderItems, coupon, discountAmount);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = requireOrderOwnedByUser(userId, orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        Coupon coupon = null;
        if (order.getUserCouponId() != null) {
            UserCoupon userCoupon = couponService.findUserCouponById(order.getUserCouponId());
            coupon = couponService.findCouponById(userCoupon.getCoupon().getId());
        }

        return toOrderResponse(order, items, coupon, order.getDiscountAmount());
    }

    @Transactional(readOnly = true)
    public PagedResult<OrderSummaryResponse> getUserOrders(Long userId, int page, int size) {
        List<Order> allOrders = orderRepository.findByUserId(userId);
        List<OrderSummaryResponse> responses = allOrders.stream()
                .map(this::toOrderSummaryResponse)
                .toList();
        return PagedResult.of(responses, page, size);
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        requireOrderOwnedByUser(userId, orderId);
        return cancelOrderInternal(orderId, reason);
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long orderId, String reason) {
        return cancelOrderInternal(orderId, reason);
    }

    private CancelOrderResponse cancelOrderInternal(Long orderId, String reason) {
        Order order = findOrderById(orderId);

        if (!order.isCancellable()) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        boolean wasPaid = order.getPaidAt() != null;

        order.cancel(reason);
        orderRepository.save(order);

        releaseStockReservations(orderId);

        if (order.getUserCouponId() != null) {
            if (wasPaid) {
                restoreCoupon(order.getUserCouponId());
            } else {
                releaseCouponReservation(order.getUserCouponId());
            }
        }

        return CancelOrderResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getCancelReason(),
                order.getCancelledAt()
        );
    }

    @Transactional
    public void completePayment(Long orderId) {
        Order order = findOrderById(orderId);

        order.markAsPaid();
        orderRepository.save(order);

        confirmStockReservations(orderId);
        incrementSalesCount(orderId);

        if (order.getUserCouponId() != null) {
            couponService.confirmCouponReservation(order.getUserCouponId());
        }
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> orderItems, Coupon coupon, Long discountAmount) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> OrderItemResponse.of(
                        item.getId(),
                        item.getProduct().getId(),
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
                order.getUser().getId(),
                order.getStatus().name(),
                itemResponses,
                pricing,
                couponResponse,
                order.getAddress(),
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
        UserCoupon userCoupon = couponService.findUserCouponById(userCouponId);

        if (!userCoupon.getUser().getId().equals(userId)) {
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
            log.warn("[Order] 쿠폰 복구 실패 - userCouponId: {}, error: {}", userCouponId, e.getMessage());
        }
    }

    private void releaseCouponReservation(Long userCouponId) {
        try {
            couponService.releaseCouponReservation(userCouponId);
        } catch (Exception e) {
            log.warn("[Order] 쿠폰 예약 해제 실패 - userCouponId: {}, error: {}", userCouponId, e.getMessage());
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
                .map(item -> item.getProduct().getId())
                .toList();
        return productService.getProductsAsMap(productIds);
    }

    private List<OrderItem> createOrderItemsWithStockReservation(List<CartItem> cartItems, Map<Long, Product> productMap) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
            }

            productService.reserveStock(product.getId(), cartItem.getQuantity());

            OrderItem orderItem = new OrderItem(
                    null,
                    product,
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.USER_NOT_FOUND));

        String orderNumber = generateOrderNumber();
        long finalAmount = itemsTotal - discountAmount;

        Order order = new Order(
                user,
                null,
                userCouponId,
                orderNumber,
                itemsTotal,
                discountAmount,
                finalAmount,
                user.getName(),
                user.getPhone(),
                "",
                deliveryAddress != null ? deliveryAddress : "",
                null,
                deliveryMemo
        );

        return order;
    }

    private String generateOrderNumber() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int)(Math.random() * 10000));
    }

    private void saveOrderItems(Order order, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            // Create new OrderItem with order reference
            OrderItem itemWithOrder = new OrderItem(
                    order,
                    item.getProduct(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice()
            );
            orderItemRepository.save(itemWithOrder);
        }
    }

    private void releaseStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            try {
                productService.releaseStockReservation(item.getProduct().getId(), item.getQuantity());
            } catch (Exception e) {
                log.warn("[Order] 재고 예약 해제 실패 - orderId: {}, productId: {}, error: {}",
                        orderId, item.getProduct().getId(), e.getMessage());
            }
        }
    }

    private void confirmStockReservations(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.confirmStockReservation(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void incrementSalesCount(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.incrementSalesCount(item.getProduct().getId(), item.getQuantity());
        }
    }

    // ========== Event Listeners ==========

    /**
     * 결제 완료 이벤트 리스너
     * PaymentService에서 결제가 성공하면 주문 상태를 PAID로 변경
     */
    @EventListener
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            completePayment(event.getOrderId());
            log.info("[Order] 결제 완료 처리 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("[Order] 결제 완료 처리 실패 - orderId: {}, error: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 이벤트 리스너
     * PaymentService에서 결제가 실패하면 주문을 취소
     */
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        try {
            cancelOrder(event.getOrderId(), event.getFailReason());
            log.info("[Order] 주문 취소 처리 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("[Order] 주문 취소 처리 실패 - orderId: {}, error: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
