package com.hhplus.ecommerce.domain.payment.model;

import com.hhplus.ecommerce.domain.payment.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 도메인 모델 테스트")
class PaymentTest {

    @Test
    @DisplayName("결제 성공 처리 시 상태가 SUCCESS로 변경되고 transactionId와 paidAt이 설정된다")
    void markAsSuccess() {
        // Given
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(50000L)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        String transactionId = "TXN-12345";

        // When
        payment.markAsSuccess(transactionId);

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
        assertThat(payment.isSuccess()).isTrue();
        assertThat(payment.isFailed()).isFalse();
    }

    @Test
    @DisplayName("결제 실패 처리 시 상태가 FAILED로 변경되고 failReason과 failedAt이 설정된다")
    void markAsFailed() {
        // Given
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(50000L)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        String failReason = "카드 한도 초과";

        // When
        payment.markAsFailed(failReason);

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(payment.getFailReason()).isEqualTo(failReason);
        assertThat(payment.getFailedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
        assertThat(payment.isSuccess()).isFalse();
        assertThat(payment.isFailed()).isTrue();
    }

    @Test
    @DisplayName("성공한 결제를 취소하면 상태가 CANCELLED로 변경된다")
    void cancel_Success() {
        // Given
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(50000L)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.SUCCESS)
                .transactionId("TXN-12345")
                .paidAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        payment.cancel();

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("성공하지 않은 결제는 취소할 수 없다 - PENDING 상태")
    void cancel_Fail_WhenPending() {
        // Given
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(50000L)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> payment.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("성공한 결제만 취소할 수 있습니다.");
    }

    @Test
    @DisplayName("성공하지 않은 결제는 취소할 수 없다 - FAILED 상태")
    void cancel_Fail_WhenFailed() {
        // Given
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(50000L)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .status(Payment.PaymentStatus.FAILED)
                .failReason("카드 한도 초과")
                .failedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> payment.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("성공한 결제만 취소할 수 있습니다.");
    }

    @Test
    @DisplayName("결제 상태 확인 - isSuccess")
    void isSuccess() {
        // Given
        Payment successPayment = Payment.builder()
                .status(Payment.PaymentStatus.SUCCESS)
                .build();
        Payment pendingPayment = Payment.builder()
                .status(Payment.PaymentStatus.PENDING)
                .build();

        // When & Then
        assertThat(successPayment.isSuccess()).isTrue();
        assertThat(pendingPayment.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("결제 상태 확인 - isFailed")
    void isFailed() {
        // Given
        Payment failedPayment = Payment.builder()
                .status(Payment.PaymentStatus.FAILED)
                .build();
        Payment successPayment = Payment.builder()
                .status(Payment.PaymentStatus.SUCCESS)
                .build();

        // When & Then
        assertThat(failedPayment.isFailed()).isTrue();
        assertThat(successPayment.isFailed()).isFalse();
    }
}
