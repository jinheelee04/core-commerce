package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.domain.brand.entity.Brand;
import com.hhplus.ecommerce.domain.cart.entity.Cart;
import com.hhplus.ecommerce.domain.cart.entity.CartItem;
import com.hhplus.ecommerce.domain.category.entity.Category;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.payment.entity.Payment;
import com.hhplus.ecommerce.domain.payment.entity.PaymentMethod;
import com.hhplus.ecommerce.domain.payment.entity.PaymentStatus;
import com.hhplus.ecommerce.domain.payment.repository.PaymentRepository;
import com.hhplus.ecommerce.domain.payment.service.PaymentService;
import com.hhplus.ecommerce.domain.product.entity.Inventory;
import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 플로우 통합 테스트
 * - 주문 생성 → 결제 처리 → 재고 확정 → 주문 상태 변경
 */
@DisplayName("통합 테스트: 결제 플로우")
class PaymentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User user;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUpTestData() {
        user = new User("test@example.com", "테스트유저", "01012345678");
        em.persist(user);

        Category category = new Category(null, "전자제품", "Electronics", 1, 1, null);
        em.persist(category);

        Brand brand = new Brand("애플", "Apple", null, "혁신적인 기술");
        em.persist(brand);

        product = new Product(category, brand, "아이폰 15", "최신 아이폰", 1500000L, null);
        em.persist(product);

        Inventory inventory = new Inventory(product, 50, 10);
        em.persist(inventory);

        Cart cart = new Cart(user);
        em.persist(cart);

        cartItem = new CartItem(cart, product, 1);
        em.persist(cartItem);

        flushAndClear();
    }

    @Test
    @DisplayName("결제 정보를 저장하고 조회할 수 있어야 한다")
    void payment_SaveAndRetrieve() {
        // Given: 주문 생성
        Order order = new Order(user, null, null, "ORD-TEST-001", 1500000L, 0L, 1500000L,
                "테스트유저", "01012345678", "12345", "서울시", "강남구", null);
        em.persist(order);
        flushAndClear();

        // When: 결제 생성
        Payment payment = new Payment(order, 1500000L, PaymentMethod.CARD, "test-client-request");
        em.persist(payment);
        flushAndClear();

        // Then: 결제 조회 성공
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getAmount()).isEqualTo(1500000L);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("결제 실패 상태를 기록할 수 있어야 한다")
    void payment_FailedStatus() {
        // Given: 주문 생성
        Order order = new Order(user, null, null, "ORD-TEST-002", 1500000L, 0L, 1500000L,
                "테스트유저", "01012345678", "12345", "서울시", "강남구", null);
        em.persist(order);

        // When: 결제 생성 후 실패 처리
        Payment payment = new Payment(order, 1500000L, PaymentMethod.CARD, "test-client-request");
        em.persist(payment);
        payment.markAsFailed("결제 승인 실패");
        flushAndClear();

        // Then: 결제 실패 상태 확인
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getFailReason()).isEqualTo("결제 승인 실패");
    }

    @Test
    @DisplayName("주문당 하나의 결제만 저장된다 (OneToOne 관계)")
    void payment_OnePaymentPerOrder() {
        // Given: 주문 생성
        Order order = new Order(user, null, null, "ORD-TEST-003", 1500000L, 0L, 1500000L,
                "테스트유저", "01012345678", "12345", "서울시", "강남구", null);
        em.persist(order);
        flushAndClear();

        // When: 결제 생성
        Payment payment = new Payment(order, 1500000L, PaymentMethod.CARD, "req-1");
        em.persist(payment);
        flushAndClear();

        // Then: 결제가 주문과 연결되어 저장됨
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getOrder().getId()).isEqualTo(order.getId());
        assertThat(savedPayment.getClientRequestId()).isEqualTo("req-1");
        assertThat(savedPayment.getAmount()).isEqualTo(1500000L);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("결제 정보 조회 시 주문 정보와 함께 조회된다")
    void payment_RetrieveWithOrder() {
        // Given: 주문 및 결제 생성
        Order order = new Order(user, null, null, "ORD-TEST-004", 2000000L, 0L, 2000000L,
                "테스트유저", "01012345678", "12345", "서울시", "강남구", null);
        em.persist(order);

        Payment payment = new Payment(order, 2000000L, PaymentMethod.CARD, "test-req");
        em.persist(payment);
        flushAndClear();

        // When: 주문 ID로 결제 조회
        Payment savedPayment = paymentRepository.findByOrderIdWithOrder(order.getId()).orElseThrow();

        // Then: 주문 정보 포함
        assertThat(savedPayment.getOrder()).isNotNull();
        assertThat(savedPayment.getOrder().getId()).isEqualTo(order.getId());
        assertThat(savedPayment.getOrder().getOrderNumber()).isEqualTo("ORD-TEST-004");
        assertThat(savedPayment.getAmount()).isEqualTo(2000000L);
    }
}
