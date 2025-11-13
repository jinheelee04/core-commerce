package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.cart.entity.Cart;
import com.hhplus.ecommerce.domain.cart.entity.CartItem;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.order.dto.OrderResponse;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.entity.OrderStatus;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.global.exception.BusinessException;
import com.hhplus.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 주문 플로우 통합 테스트
 * - 장바구니 → 주문 생성 → 재고 예약 → 주문 조회
 */
@DisplayName("통합 테스트: 주문 플로우")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private User user;
    private Product product;
    private Product product2;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUpTestData() {
        // Given: 테스트 데이터 준비
        user = new User("test@example.com", "테스트유저", "01012345678");
        em.persist(user);

        Category category = new Category(null, "전자제품", "Electronics", 1, 1, null);
        em.persist(category);

        Brand brand = new Brand("삼성", "Samsung", null, "글로벌 전자제품 브랜드");
        em.persist(brand);

        product = new Product(category, brand, "갤럭시 S24", "최신 스마트폰", 1200000L, null);
        em.persist(product);

        product2 = new Product(category, brand, "갤럭시 S21", "최신 스마트폰", 1200000L, null);
        em.persist(product2);

        Inventory inventory = new Inventory(product, 100, 10);
        em.persist(inventory);

        cart = new Cart(user);
        em.persist(cart);

        cartItem = new CartItem(cart, product, 2);
        em.persist(cartItem);

        flushAndClear();
    }

    @Test
    @DisplayName("주문 생성 시 재고가 예약되고 주문 상태가 PENDING이어야 한다")
    void createOrder_Success() {
        // When: 주문 생성
        OrderResponse response = orderService.createOrder(
                user.getId(),
                List.of(cartItem.getId()),
                null,
                "서울시 강남구 테헤란로 123",
                "문 앞에 놓아주세요"
        );

        flushAndClear();

        // Then: 주문이 정상 생성되고 재고가 예약됨
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.items()).hasSize(1);
        assertThat(response.pricing().itemsTotal()).isEqualTo(2400000L);

        // 재고 확인
        Inventory inventory = em.createQuery(
                "SELECT i FROM Inventory i WHERE i.product.id = :productId", Inventory.class)
                .setParameter("productId", product.getId())
                .getSingleResult();

        assertThat(inventory.getReservedStock()).isEqualTo(2);
        assertThat(inventory.getAvailableStock()).isEqualTo(98);
    }

    @Test
    @Transactional
    @DisplayName("재고가 부족하면 주문 생성이 실패해야 한다")
    void createOrder_InsufficientStock_Fail() {
        // Given
        CartItem largeQuantityItem = new CartItem(cart, product2, 150);
        em.persist(largeQuantityItem);
        flushAndClear();

        // When & Then
        assertThatThrownBy(() ->
                orderService.createOrder(
                        user.getId(),
                        List.of(largeQuantityItem.getId()),
                        null,
                        "서울시 강남구",
                        null
                )
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("주문 취소 시 재고가 복구되어야 한다")
    void cancelOrder_RestoreStock() {
        // Given: 주문 생성
        OrderResponse orderResponse = orderService.createOrder(
                user.getId(),
                List.of(cartItem.getId()),
                null,
                "서울시 강남구",
                null
        );
        flushAndClear();

        // When: 주문 취소
        orderService.cancelOrder(user.getId(), orderResponse.orderId(), "단순 변심");
        flushAndClear();

        // Then: 재고가 복구됨
        Inventory inventory = em.createQuery(
                "SELECT i FROM Inventory i WHERE i.product.id = :productId", Inventory.class)
                .setParameter("productId", product.getId())
                .getSingleResult();

        assertThat(inventory.getReservedStock()).isEqualTo(0);
        assertThat(inventory.getAvailableStock()).isEqualTo(100);

        // 주문 상태 확인
        Order order = orderRepository.findById(orderResponse.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 조회 시 모든 연관 데이터가 포함되어야 한다")
    void getOrder_WithAllRelations() {
        // Given: 주문 생성
        OrderResponse createdOrder = orderService.createOrder(
                user.getId(),
                List.of(cartItem.getId()),
                null,
                "서울시 강남구",
                "배송 메모"
        );
        flushAndClear();

        // When: 주문 조회
        OrderResponse response = orderService.getOrder(user.getId(), createdOrder.orderId());

        // Then: 모든 데이터가 포함됨
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("갤럭시 S24");
        assertThat(response.deliveryAddress()).isEqualTo("서울시 강남구");
        assertThat(response.deliveryMemo()).isEqualTo("배송 메모");
    }

    @Test
    @DisplayName("다른 사용자의 주문은 조회할 수 없어야 한다")
    void getOrder_OtherUser_Fail() {
        // Given: 주문 생성
        OrderResponse orderResponse = orderService.createOrder(
                user.getId(),
                List.of(cartItem.getId()),
                null,
                "서울시 강남구",
                null
        );

        User otherUser = new User("other@example.com", "다른유저", "01098765432");
        em.persist(otherUser);
        flushAndClear();

        // When & Then: 다른 사용자의 주문 조회 실패
        assertThatThrownBy(() ->
                orderService.getOrder(otherUser.getId(), orderResponse.orderId())
        ).isInstanceOf(BusinessException.class);
    }
}
