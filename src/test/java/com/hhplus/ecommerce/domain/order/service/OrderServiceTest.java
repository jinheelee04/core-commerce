package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.entity.OrderStatus;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("OrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.hhplus.ecommerce.domain.order.repository.OrderItemRepository orderItemRepository;

    @Mock
    private com.hhplus.ecommerce.domain.user.repository.UserRepository userRepository;

    @Mock
    private com.hhplus.ecommerce.domain.cart.service.CartService cartService;

    @Mock
    private com.hhplus.ecommerce.domain.product.service.ProductService productService;

    @Mock
    private com.hhplus.ecommerce.domain.coupon.service.CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 200L;

    private User mockUser;
    private Product mockProduct;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(USER_ID);

        Category mockCategory = mock(Category.class);
        Brand mockBrand = mock(Brand.class);

        mockProduct = new Product(mockCategory, mockBrand, "테스트 상품", "설명", 10000L, null);
        setId(mockProduct, PRODUCT_ID);

        mockOrder = new Order(mockUser, null, null, "ORD-001", 20000L, 0L, 20000L,
                "홍길동", "01012345678", "12345", "서울시", "강남구", null);
        setId(mockOrder, ORDER_ID);
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // === 주문 조회 테스트 ===

    @Test
    @DisplayName("주문 ID로 조회 성공")
    void findOrderById_Success() {
        // Given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mockOrder));

        // When
        Order result = orderService.findOrderById(ORDER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("주문 ID로 조회 실패 - 존재하지 않는 주문")
    void findOrderById_NotFound() {
        // Given
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.findOrderById(ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 주문 목록 조회 성공")
    void getUserOrders_Success() {
        // Given
        given(orderRepository.findByUserId(USER_ID)).willReturn(List.of(mockOrder));

        // When
        var result = orderService.getUserOrders(USER_ID, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() {
        // Given
        given(orderRepository.findByIdWithUser(ORDER_ID)).willReturn(Optional.of(mockOrder));
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mockOrder));
        given(orderItemRepository.findByOrderId(ORDER_ID)).willReturn(List.of());

        // When
        orderService.cancelOrder(USER_ID, ORDER_ID, "단순 변심");

        // Then
        assertThat(mockOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, times(1)).save(mockOrder);
    }

    @Test
    @DisplayName("주문 취소 실패 - 이미 취소된 주문")
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        // Given
        mockOrder.cancel("이미 취소됨");
        given(orderRepository.findByIdWithUser(ORDER_ID)).willReturn(Optional.of(mockOrder));
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(mockOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID, "단순 변심"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 다른 사용자의 주문")
    void cancelOrder_AccessDenied_ThrowsException() {
        // Given
        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(999L);

        Order otherOrder = new Order(otherUser, null, null, "ORD-003", 20000L, 0L, 20000L,
                "홍길동", "01012345678", "12345", "서울시", "강남구", null);
        setId(otherOrder, ORDER_ID);

        given(orderRepository.findByIdWithUser(ORDER_ID)).willReturn(Optional.of(otherOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID, "단순 변심"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_ACCESS_DENIED);
    }
}
