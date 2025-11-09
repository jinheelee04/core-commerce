package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.service.CartService;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.dto.CancelOrderResponse;
import com.hhplus.ecommerce.domain.order.dto.OrderResponse;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderItem;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import com.hhplus.ecommerce.domain.order.repository.OrderItemRepository;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.product.model.product.Product;
import com.hhplus.ecommerce.domain.product.model.product.ProductCategory;
import com.hhplus.ecommerce.domain.product.model.product.ProductStatus;
import com.hhplus.ecommerce.domain.product.service.ProductService;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    private Product testProduct;
    private CartItem testCartItem;
    private Coupon testCoupon;
    private UserCoupon testUserCoupon;
    
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        testProduct = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testCartItem = CartItem.create(1L, 1L, 1L, "테스트 상품", 10000L, 2);

        testCoupon = Coupon.builder()
                .id(1L)
                .code("WELCOME10")
                .name("신규 회원 쿠폰")
                .description("10% 할인")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(50)
                .startsAt(now.minusDays(1))
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        testUserCoupon = UserCoupon.builder()
                .id(1L)
                .couponId(1L)
                .userId(1L)
                .isUsed(false)
                .issuedAt(now)
                .expiresAt(now.plusDays(30))
                .updatedAt(now)
                .build();
    }
    
    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 미적용")
    void createOrder_WithoutCoupon_Success() {
        // given
        Long userId = 1L;
        List<Long> cartItemIds = List.of(1L);

        when(cartService.getCartItemsByIds(userId, cartItemIds)).thenReturn(List.of(testCartItem));
        when(productService.getProductsAsMap(any())).thenReturn(Map.of(1L, testProduct));
        when(orderRepository.generateNextId()).thenReturn(1L);
        when(orderRepository.generateOrderNumber()).thenReturn("ORD-001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.generateNextId()).thenReturn(1L);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        OrderResponse result = orderService.createOrder(userId, cartItemIds, null, "서울", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.pricing().discountAmount()).isEqualTo(0L);
        assertThat(result.coupon()).isNull();
        verify(productService).reserveStock(1L, 2);
        verify(cartService).removeCartItems(cartItemIds);
        verify(couponService, never()).useCoupon(any(), any());
    }

    @Test
    @DisplayName("주문 생성 성공 - 쿠폰 적용")
    void createOrder_WithCoupon_Success() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        List<Long> cartItemIds = List.of(1L);

        when(cartService.getCartItemsByIds(userId, cartItemIds)).thenReturn(List.of(testCartItem));
        when(productService.getProductsAsMap(any())).thenReturn(Map.of(1L, testProduct));
        when(couponService.findUserCouponById(userCouponId)).thenReturn(testUserCoupon);
        when(couponService.findCouponById(1L)).thenReturn(testCoupon);
        when(orderRepository.generateNextId()).thenReturn(1L);
        when(orderRepository.generateOrderNumber()).thenReturn("ORD-001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.generateNextId()).thenReturn(1L);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        OrderResponse result = orderService.createOrder(userId, cartItemIds, userCouponId, "서울", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.pricing().itemsTotal()).isEqualTo(20000L); // 10000 * 2
        assertThat(result.pricing().discountAmount()).isEqualTo(2000L); // 20000 * 10%
        assertThat(result.pricing().finalAmount()).isEqualTo(18000L); // 20000 - 2000
        assertThat(result.coupon()).isNotNull();
        assertThat(result.coupon().name()).isEqualTo("신규 회원 쿠폰");
        verify(productService).reserveStock(1L, 2);
        verify(cartService).removeCartItems(cartItemIds);
        verify(couponService, never()).useCoupon(any(), any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 다른 사용자의 쿠폰 사용 시도")
    void createOrder_InvalidCouponOwner_ThrowsException() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        List<Long> cartItemIds = List.of(1L);
        UserCoupon otherUserCoupon = UserCoupon.builder()
                .id(1L)
                .couponId(1L)
                .userId(999L) // 다른 사용자
                .isUsed(false)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.getCartItemsByIds(userId, cartItemIds)).thenReturn(List.of(testCartItem));
        when(productService.getProductsAsMap(any())).thenReturn(Map.of(1L, testProduct));
        when(couponService.findUserCouponById(userCouponId)).thenReturn(otherUserCoupon);
        when(orderItemRepository.generateNextId()).thenReturn(1L);

        // when & then
        assertThatThrownBy(() ->
                orderService.createOrder(userId, cartItemIds, userCouponId, "서울", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INVALID_COUPON_OWNER);

        // 재고는 예약되었으나 주문 생성 전 실패했으므로 재고는 예약된 상태
        verify(productService).reserveStock(1L, 2);
        verify(couponService, never()).useCoupon(any(), any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 이미 사용된 쿠폰")
    void createOrder_AlreadyUsedCoupon_ThrowsException() {
        // given
        Long userId = 1L;
        Long userCouponId = 1L;
        List<Long> cartItemIds = List.of(1L);
        UserCoupon usedCoupon = UserCoupon.builder()
                .id(1L)
                .couponId(1L)
                .userId(1L)
                .isUsed(true) // 이미 사용됨
                .issuedAt(LocalDateTime.now())
                .usedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.getCartItemsByIds(userId, cartItemIds)).thenReturn(List.of(testCartItem));
        when(productService.getProductsAsMap(any())).thenReturn(Map.of(1L, testProduct));
        when(couponService.findUserCouponById(userCouponId)).thenReturn(usedCoupon);
        when(orderItemRepository.generateNextId()).thenReturn(1L);

        // when & then
        assertThatThrownBy(() ->
                orderService.createOrder(userId, cartItemIds, userCouponId, "서울", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.COUPON_NOT_USABLE);

        verify(couponService, never()).useCoupon(any(), any());
    }

    @Test
    @DisplayName("주문 취소 성공 - PENDING 상태 (쿠폰 복구 불필요)")
    void cancelOrder_PendingStatus_NoCouponRestore() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long userCouponId = 1L;
        String reason = "단순 변심";

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .orderNumber("ORD-001")
                .status(OrderStatus.PENDING)
                .itemsTotal(20000L)
                .discountAmount(2000L)
                .finalAmount(18000L)
                .userCouponId(userCouponId)
                .deliveryAddress("서울")
                .paidAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        // when
        CancelOrderResponse result = orderService.cancelOrder(userId, orderId, reason);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(result.cancelReason()).isEqualTo(reason);
        verify(couponService, never()).cancelCouponUse(any());
    }

    @Test
    @DisplayName("주문 취소 성공 - PAID 상태 (쿠폰 복구 필요)")
    void cancelOrder_PaidStatus_RestoreCoupon() {
        // given
        Long orderId = 1L;
        Long userId = 1L;
        Long userCouponId = 1L;
        String reason = "관리자 취소";

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .orderNumber("ORD-001")
                .status(OrderStatus.PAID)
                .itemsTotal(20000L)
                .discountAmount(2000L)
                .finalAmount(18000L)
                .userCouponId(userCouponId)
                .deliveryAddress("서울")
                .paidAt(LocalDateTime.now())  // PAID 상태 표시
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        // when
        CancelOrderResponse result = orderService.cancelOrder(userId, orderId, reason);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(result.cancelReason()).isEqualTo(reason);
        verify(couponService).cancelCouponUse(userCouponId);  // PAID 상태에서는 쿠폰 복구 필요
    }

    @Test
    @DisplayName("주문 생성 실패 - 빈 장바구니")
    void createOrder_EmptyCart_ThrowsException() {
        // given
        when(cartService.getCartItemsByIds(any(), any())).thenReturn(List.of());

        // when & then
        assertThatThrownBy(() ->
                orderService.createOrder(1L, List.of(1L), null, "서울", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INVALID_ORDER_REQUEST);
    }
}
