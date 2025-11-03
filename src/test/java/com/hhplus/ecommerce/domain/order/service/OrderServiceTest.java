package com.hhplus.ecommerce.domain.order.service;

import com.hhplus.ecommerce.domain.cart.model.CartItem;
import com.hhplus.ecommerce.domain.cart.service.CartService;
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
import com.hhplus.ecommerce.global.common.exception.BusinessException;
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
    
    @InjectMocks
    private OrderService orderService;
    
    private Product testProduct;
    private CartItem testCartItem;
    
    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .price(10000L)
                .category(ProductCategory.ELECTRONICS)
                .status(ProductStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        testCartItem = CartItem.create(1L, 1L, 1L, "테스트 상품", 10000L, 2);
    }
    
    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
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
        OrderResponse result = orderService.createOrder(userId, cartItemIds, "서울", null);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        verify(productService).reserveStock(1L, 2);
        verify(cartService).removeCartItems(cartItemIds);
    }
    
    @Test
    @DisplayName("주문 생성 실패 - 빈 장바구니")
    void createOrder_EmptyCart_ThrowsException() {
        // given
        when(cartService.getCartItemsByIds(any(), any())).thenReturn(List.of());
        
        // when & then
        assertThatThrownBy(() -> orderService.createOrder(1L, List.of(1L), "서울", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INVALID_ORDER_REQUEST);
    }
}
