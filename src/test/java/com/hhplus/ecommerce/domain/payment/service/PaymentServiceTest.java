package com.hhplus.ecommerce.domain.payment.service;

import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.event.PaymentCompletedEvent;
import com.hhplus.ecommerce.domain.payment.event.PaymentFailedEvent;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import com.hhplus.ecommerce.domain.payment.model.Payment;
import com.hhplus.ecommerce.domain.payment.model.PaymentMethod;
import com.hhplus.ecommerce.domain.payment.model.PaymentStatus;
import com.hhplus.ecommerce.domain.payment.repository.PaymentRepository;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.common.exception.DomainExceptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock(lenient = true)
    private RestClient restClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private CouponService couponService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock(lenient = true)
    private DomainExceptionMapper exceptionMapper;

    @Mock(lenient = true)
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock(lenient = true)
    private RequestBodySpec requestBodySpec;

    @Mock(lenient = true)
    private ResponseSpec responseSpec;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PAYMENT_ID = 200L;
    private static final Long AMOUNT = 50000L;
    private static final String CLIENT_REQUEST_ID = "req-12345";
    private static final String TRANSACTION_ID = "TXN-ABCD1234";

    @BeforeEach
    void setUp() {
        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(nullable(String.class))).willReturn(requestBodySpec);
        given(requestBodySpec.body(anyMap())).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);

        given(exceptionMapper.mapToPaymentException(any(BusinessException.class), anyString()))
                .willAnswer(invocation -> {
                    BusinessException e = invocation.getArgument(0);
                    if (e.getErrorCode() == OrderErrorCode.ORDER_NOT_FOUND) {
                        return new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                    } else if (e.getErrorCode() == OrderErrorCode.ORDER_ACCESS_DENIED) {
                        return new BusinessException(PaymentErrorCode.PAYMENT_NOT_ALLOWED);
                    }
                    return e;
                });
    }

    private Order createTestOrder() {
        return createTestOrder(USER_ID, ORDER_ID, OrderStatus.PENDING);
    }

    private Order createTestOrder(Long userId, Long orderId, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .userId(userId)
                .orderNumber("ORD-20250104-001")
                .status(status)
                .itemsTotal(AMOUNT)
                .discountAmount(0L)
                .finalAmount(AMOUNT)
                .deliveryAddress("서울시 강남구")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Payment createSuccessPayment() {
        return createSuccessPayment(PAYMENT_ID, ORDER_ID, TRANSACTION_ID);
    }

    private Payment createSuccessPayment(Long paymentId, Long orderId, String transactionId) {
        return Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .amount(AMOUNT)
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCESS)
                .clientRequestId(CLIENT_REQUEST_ID)
                .transactionId(transactionId)
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> createSuccessPgResponse() {
        return Map.of(
                "success", true,
                "transactionId", TRANSACTION_ID,
                "orderId", ORDER_ID,
                "amount", AMOUNT,
                "message", "결제가 성공적으로 처리되었습니다"
        );
    }

    private Map<String, Object> createFailedPgResponse(String reason) {
        return Map.of(
                "success", false,
                "orderId", ORDER_ID,
                "amount", AMOUNT,
                "message", reason
        );
    }


    @Test
    @DisplayName("정상 결제 성공 - PG 성공 응답 시 결제 상태 SUCCESS, 주문 완료 처리")
    void processPayment_Success() {
        // Given
        Order testOrder = createTestOrder();
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentRepository.findByTransactionId(TRANSACTION_ID)).willReturn(Optional.empty());
        given(responseSpec.body(eq(Map.class))).willReturn(createSuccessPgResponse());

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.amount()).isEqualTo(AMOUNT);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(response.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.failReason()).isNull();

        // 이벤트 발행 검증
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(event.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.getTransactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    @DisplayName("PG 결제 실패 - PG 실패 응답 시 결제 상태 FAILED, 주문 취소 처리")
    void processPayment_PgFailed() {
        // Given
        Order testOrder = createTestOrder();
        String failReason = "카드 한도 초과";

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(responseSpec.body(eq(Map.class))).willReturn(createFailedPgResponse(failReason));

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED.name());
        assertThat(response.failReason()).isEqualTo(failReason);
        assertThat(response.transactionId()).isNull();

        // 이벤트 발행 검증
        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentFailedEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(event.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.getFailReason()).isEqualTo("결제 실패: " + failReason);
    }

    @Test
    @DisplayName("멱등성 - 동일 clientRequestId 재요청 시 기존 응답 반환")
    void processPayment_IdempotencyByClientRequestId() {
        // Given
        Payment existingPayment = createSuccessPayment();
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.of(existingPayment));

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(response.transactionId()).isEqualTo(TRANSACTION_ID);

        verify(paymentRepository, never()).save(any());
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("멱등성 - 동일 주문에 성공한 결제가 있으면 기존 응답 반환")
    void processPayment_IdempotencyByOrderId() {
        // Given
        Order testOrder = createTestOrder();
        Payment successfulPayment = createSuccessPayment();

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(successfulPayment));

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());

        verify(paymentRepository, never()).save(any());
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("멱등성 - PG에서 중복 transactionId 반환 시 현재 결제를 FAILED로 처리")
    void processPayment_DuplicateTransactionId() {
        // Given
        Order testOrder = createTestOrder();
        Payment existingPaymentByTxId = createSuccessPayment(999L, 888L, TRANSACTION_ID);

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentRepository.findByTransactionId(TRANSACTION_ID)).willReturn(Optional.of(existingPaymentByTxId));
        given(responseSpec.body(eq(Map.class))).willReturn(createSuccessPgResponse());

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED.name());
        assertThat(response.failReason()).contains("이미 동일한 transactionId");

        // 이벤트가 발행되지 않아야 함
        verify(eventPublisher, never()).publishEvent(any(PaymentCompletedEvent.class));
    }

    // ========== 예외 시나리오 테스트 ==========

    @Test
    @DisplayName("주문을 찾을 수 없는 경우 - ORDER_NOT_FOUND 예외 발생")
    void processPayment_OrderNotFound() {
        // Given
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        willThrow(new BusinessException(OrderErrorCode.ORDER_NOT_FOUND))
                .given(orderService).requireOrderOwnedByUser(USER_ID, ORDER_ID);

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_FOUND);

        verify(paymentRepository, never()).save(any());
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("주문 소유자가 다른 경우 - ORDER_ACCESS_DENIED 예외 발생")
    void processPayment_OrderAccessDenied() {
        // Given
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        willThrow(new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED))
                .given(orderService).requireOrderOwnedByUser(USER_ID, ORDER_ID);

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_ALLOWED);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 상태가 PENDING이 아닌 경우 - INVALID_ORDER_STATUS 예외 발생")
    void processPayment_InvalidOrderStatus() {
        // Given
        Order paidOrder = createTestOrder(USER_ID, ORDER_ID, OrderStatus.PAID);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(paidOrder);

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.INVALID_ORDER_STATUS);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("PG 호출 중 예외 발생 - 결제 실패 처리 및 주문 취소")
    void processPayment_PgException() {
        // Given
        Order testOrder = createTestOrder();
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(responseSpec.body(eq(Map.class))).willThrow(new RuntimeException("PG 연동 오류"));

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_FAILED);

        // 이벤트 발행 검증
        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentFailedEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(event.getFailReason()).isEqualTo("결제 처리 중 오류 발생");
    }

    // ========== 결제 조회 테스트 ==========

    @Test
    @DisplayName("결제 조회 성공")
    void getPayment_Success() {
        // Given
        Payment savedPayment = createSuccessPayment();
        Order testOrder = createTestOrder();

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(savedPayment));
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);

        // When
        PaymentResponse response = paymentService.getPayment(USER_ID, PAYMENT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());
    }

    @Test
    @DisplayName("결제 조회 시 결제를 찾을 수 없는 경우 - PAYMENT_NOT_FOUND 예외 발생")
    void getPayment_NotFound() {
        // Given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPayment(USER_ID, PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("주문별 결제 조회 성공")
    void getPaymentByOrderId_Success() {
        // Given
        Payment savedPayment = createSuccessPayment();
        Order testOrder = createTestOrder();

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(testOrder);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(savedPayment));

        // When
        PaymentResponse response = paymentService.getPaymentByOrderId(USER_ID, ORDER_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("주문별 결제 조회 시 주문 소유자가 다르면 - PAYMENT_NOT_ALLOWED 예외 발생")
    void getPaymentByOrderId_AccessDenied() {
        // Given
        willThrow(new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED))
                .given(orderService).requireOrderOwnedByUser(USER_ID, ORDER_ID);

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_ALLOWED);
    }

    // ========== 쿠폰 관련 테스트 ==========

    @Test
    @DisplayName("쿠폰이 적용된 주문 결제 성공 - 응답에 쿠폰 정보 포함")
    void processPayment_WithCoupon_Success() {
        // Given
        Long userCouponId = 10L;
        Long couponId = 5L;
        Long discountAmount = 5000L;
        String couponName = "신규 회원 할인 쿠폰";

        Order orderWithCoupon = Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .orderNumber("ORD-20250104-002")
                .status(OrderStatus.PENDING)
                .itemsTotal(50000L)
                .discountAmount(discountAmount)
                .finalAmount(45000L)
                .userCouponId(userCouponId)
                .deliveryAddress("서울시 강남구")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserCoupon userCoupon = UserCoupon.builder()
                .id(userCouponId)
                .couponId(couponId)
                .userId(USER_ID)
                .isUsed(true)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .code("WELCOME10")
                .name(couponName)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(10000L)
                .totalQuantity(100)
                .remainingQuantity(50)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(orderWithCoupon);
        given(orderService.findOrderById(ORDER_ID)).willReturn(orderWithCoupon);
        given(couponService.findUserCouponById(userCouponId)).willReturn(userCoupon);
        given(couponService.findCouponById(couponId)).willReturn(coupon);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(paymentRepository.findByTransactionId(TRANSACTION_ID)).willReturn(Optional.empty());
        given(responseSpec.body(eq(Map.class))).willReturn(createSuccessPgResponse());

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(response.couponInfo()).isNotNull();
        assertThat(response.couponInfo().couponId()).isEqualTo(couponId);
        assertThat(response.couponInfo().couponName()).isEqualTo(couponName);
        assertThat(response.couponInfo().discountAmount()).isEqualTo(discountAmount);

        // 이벤트 발행 검증
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("쿠폰이 적용된 주문 결제 실패 - 주문 취소로 쿠폰 복구")
    void processPayment_WithCoupon_Failed_RestoresCoupon() {
        // Given
        Long userCouponId = 10L;
        String failReason = "카드 승인 거부";

        Order orderWithCoupon = Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .orderNumber("ORD-20250104-003")
                .status(OrderStatus.PENDING)
                .itemsTotal(50000L)
                .discountAmount(5000L)
                .finalAmount(45000L)
                .userCouponId(userCouponId)
                .deliveryAddress("서울시 강남구")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(orderWithCoupon);
        given(paymentRepository.findByClientRequestId(CLIENT_REQUEST_ID)).willReturn(Optional.empty());
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
        given(paymentRepository.generateNextId()).willReturn(PAYMENT_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(responseSpec.body(eq(Map.class))).willReturn(createFailedPgResponse(failReason));

        // When
        PaymentResponse response = paymentService.processPayment(
                USER_ID, ORDER_ID, PaymentMethod.CARD, CLIENT_REQUEST_ID
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED.name());
        assertThat(response.failReason()).isEqualTo(failReason);

        // 이벤트 발행 검증 - 주문 취소 시 쿠폰도 자동 복구됨
        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentFailedEvent event = eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(event.getFailReason()).isEqualTo("결제 실패: " + failReason);
    }

    @Test
    @DisplayName("결제 조회 시 쿠폰 정보 포함")
    void getPayment_WithCoupon_IncludesCouponInfo() {
        // Given
        Long userCouponId = 10L;
        Long couponId = 5L;
        Long discountAmount = 3000L;
        String couponName = "첫 구매 할인";

        Payment savedPayment = createSuccessPayment();

        Order orderWithCoupon = Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .orderNumber("ORD-20250104-004")
                .status(OrderStatus.PAID)
                .itemsTotal(30000L)
                .discountAmount(discountAmount)
                .finalAmount(27000L)
                .userCouponId(userCouponId)
                .deliveryAddress("서울시 강남구")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserCoupon userCoupon = UserCoupon.builder()
                .id(userCouponId)
                .couponId(couponId)
                .userId(USER_ID)
                .isUsed(true)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .code("FIRST3000")
                .name(couponName)
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(3000)
                .minOrderAmount(20000L)
                .totalQuantity(50)
                .remainingQuantity(25)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(savedPayment));
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willReturn(orderWithCoupon);
        given(orderService.findOrderById(ORDER_ID)).willReturn(orderWithCoupon);
        given(couponService.findUserCouponById(userCouponId)).willReturn(userCoupon);
        given(couponService.findCouponById(couponId)).willReturn(coupon);

        // When
        PaymentResponse response = paymentService.getPayment(USER_ID, PAYMENT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.couponInfo()).isNotNull();
        assertThat(response.couponInfo().couponId()).isEqualTo(couponId);
        assertThat(response.couponInfo().couponName()).isEqualTo(couponName);
        assertThat(response.couponInfo().discountAmount()).isEqualTo(discountAmount);
    }
}
