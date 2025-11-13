package com.hhplus.ecommerce.domain.payment.service;

import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.entity.Payment;
import com.hhplus.ecommerce.domain.payment.entity.PaymentMethod;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import com.hhplus.ecommerce.domain.payment.repository.PaymentRepository;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.hhplus.ecommerce.global.exception.DomainExceptionMapper exceptionMapper;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PAYMENT_ID = 200L;
    private static final Long AMOUNT = 50000L;

    private User mockUser;
    private Order mockOrder;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(USER_ID);

        mockOrder = mock(Order.class);
        when(mockOrder.getId()).thenReturn(ORDER_ID);
        when(mockOrder.getUser()).thenReturn(mockUser);
        when(mockOrder.getFinalAmount()).thenReturn(AMOUNT);
        when(mockOrder.getOrderNumber()).thenReturn("ORD-001");

        mockPayment = new Payment(mockOrder, AMOUNT, PaymentMethod.CARD, "req-123");
        setId(mockPayment, PAYMENT_ID);
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

    // === 결제 조회 테스트 ===

    @Test
    @DisplayName("결제 ID로 조회 성공")
    void findPaymentById_Success() {
        // Given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(mockPayment));

        // When
        Payment result = paymentService.findPaymentById(PAYMENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
    }

    @Test
    @DisplayName("결제 ID로 조회 실패 - 존재하지 않는 결제")
    void findPaymentById_NotFound() {
        // Given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.findPaymentById(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자의 결제 조회 성공")
    void getPayment_Success() {
        // Given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(mockPayment));

        // When
        PaymentResponse result = paymentService.getPayment(USER_ID, PAYMENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(result.amount()).isEqualTo(AMOUNT);
    }

    @Test
    @DisplayName("사용자의 결제 조회 실패 - 존재하지 않는 결제")
    void getPayment_NotFound() {
        // Given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPayment(USER_ID, PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자의 결제 조회 실패 - 다른 사용자의 결제")
    void getPayment_AccessDenied() {
        // Given
        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(999L);

        Order otherOrder = mock(Order.class);
        when(otherOrder.getId()).thenReturn(ORDER_ID);
        when(otherOrder.getUser()).thenReturn(otherUser);

        Payment otherPayment = new Payment(otherOrder, AMOUNT, PaymentMethod.CARD, "req-456");
        setId(otherPayment, PAYMENT_ID);

        BusinessException orderException = new BusinessException(com.hhplus.ecommerce.domain.order.exception.OrderErrorCode.ORDER_ACCESS_DENIED);
        BusinessException paymentException = new BusinessException(PaymentErrorCode.PAYMENT_NOT_ALLOWED);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(otherPayment));
        given(orderService.requireOrderOwnedByUser(USER_ID, ORDER_ID)).willThrow(orderException);
        given(exceptionMapper.mapToPaymentException(any(BusinessException.class), any(String.class))).willReturn(paymentException);

        // When & Then
        assertThatThrownBy(() -> paymentService.getPayment(USER_ID, PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("주문 ID로 결제 조회 성공")
    void getPaymentByOrderId_Success() {
        // Given
        given(paymentRepository.findByOrderIdWithOrder(ORDER_ID)).willReturn(Optional.of(mockPayment));

        // When
        PaymentResponse result = paymentService.getPaymentByOrderId(USER_ID, ORDER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("주문 ID로 결제 조회 실패 - 존재하지 않는 결제")
    void getPaymentByOrderId_NotFound() {
        // Given
        given(paymentRepository.findByOrderIdWithOrder(ORDER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(USER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 저장 성공")
    void savePayment_Success() {
        // Given
        Payment newPayment = new Payment(mockOrder, AMOUNT, PaymentMethod.CARD, "req-789");
        given(paymentRepository.save(any(Payment.class))).willReturn(mockPayment);

        // When
        Payment result = paymentRepository.save(newPayment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
