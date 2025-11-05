package com.hhplus.ecommerce.domain.payment.service;

import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.model.Order;
import com.hhplus.ecommerce.domain.order.model.OrderStatus;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import com.hhplus.ecommerce.domain.payment.model.Payment;
import com.hhplus.ecommerce.domain.payment.model.PaymentMethod;
import com.hhplus.ecommerce.domain.payment.repository.PaymentRepository;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final RestClient restClient;

    @Value("${mock.payment.url}")
    private String mockPaymentUrl;

    public Payment getPaymentEntity(Long paymentId){
        return  paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public PaymentResponse processPayment(Long userId, Long orderId, PaymentMethod paymentMethod, String clientRequestId) {
        log.info("[Payment] 결제 요청 시작 - userId: {}, orderId: {}, method: {}, clientRequestId: {}",
                userId, orderId, paymentMethod, clientRequestId);

        Payment duplicate = checkDuplicateRequest(clientRequestId);
        if (duplicate != null) {
            log.info("[Payment] 중복 요청 감지 - clientRequestId: {}, 기존 paymentId: {}", clientRequestId, duplicate.getId());
            return toPaymentResponse(duplicate);
        }

        Order order = validateOrderForPayment(userId, orderId);

        Payment existingPayment = checkExistingSuccessfulPayment(orderId);
        if (existingPayment != null) {
            log.info("[Payment] 기존 성공 결제 존재 - orderId: {}, paymentId: {}", orderId, existingPayment.getId());
            return toPaymentResponse(existingPayment);
        }

        Payment payment = createPendingPayment(orderId, order.getFinalAmount(), paymentMethod, clientRequestId);
        paymentRepository.save(payment);
        log.debug("[Payment] PENDING 상태 결제 생성 완료 - paymentId: {}, orderId: {}", payment.getId(), orderId);

        executePayment(order, payment, paymentMethod);

        log.info("[Payment] 결제 처리 완료 - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
        return toPaymentResponse(payment);
    }

    public PaymentResponse getPayment(Long userId, Long paymentId) {
        log.debug("[Payment] 결제 조회 요청 - userId: {}, paymentId: {}", userId, paymentId);

        Payment payment = getPaymentEntity(paymentId);
        validateOrderOwnership(userId, payment.getOrderId(), "결제 조회");

        return toPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long userId, Long orderId) {
        log.debug("[Payment] 주문별 결제 조회 요청 - userId: {}, orderId: {}", userId, orderId);

        validateOrderOwnership(userId, orderId, "결제 조회");
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return toPaymentResponse(payment);
    }

    // ========== Private Helper Methods ==========

    private Payment checkDuplicateRequest(String clientRequestId) {
        if (clientRequestId == null) return null;
        return paymentRepository.findByClientRequestId(clientRequestId).orElse(null);
    }

    private void validateOrderOwnership(Long userId, Long orderId, String context) {
        try {
            orderService.requireOrderOwnedByUser(userId, orderId);
        } catch (BusinessException e) {
            throw convertOrderExceptionToPaymentException(e, context);
        }
    }

    private Order validateOrderForPayment(Long userId, Long orderId) {
        log.debug("[Payment] 주문 유효성 검증 시작 - userId: {}, orderId: {}", userId, orderId);

        Order order;
        try {
            order = orderService.requireOrderOwnedByUser(userId, orderId);
        } catch (BusinessException e) {
            throw convertOrderExceptionToPaymentException(e, "결제 처리");
        }

        if (order == null) {
            log.error("[Payment] 주문 조회 결과가 null - orderId: {}", orderId);
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[Payment] 유효하지 않은 주문 상태 - orderId: {}, status: {}", orderId, order.getStatus());
            throw new BusinessException(PaymentErrorCode.INVALID_ORDER_STATUS);
        }

        log.debug("[Payment] 주문 유효성 검증 완료 - orderId: {}, amount: {}", orderId, order.getFinalAmount());
        return order;
    }

    private BusinessException convertOrderExceptionToPaymentException(BusinessException e, String context) {
        if (e.getErrorCode() == OrderErrorCode.ORDER_NOT_FOUND) {
            log.warn("[Payment] {} 실패: 주문을 찾을 수 없음", context);
            return new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        } else if (e.getErrorCode() == OrderErrorCode.ORDER_ACCESS_DENIED) {
            log.warn("[Payment] {} 실패: 주문 접근 권한 없음", context);
            return new BusinessException(PaymentErrorCode.PAYMENT_NOT_ALLOWED);
        }
        return e;
    }

    private Payment checkExistingSuccessfulPayment(Long orderId) {
        Payment existingPayment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existingPayment != null && existingPayment.isSuccess()) {
            return existingPayment;
        }
        return null;
    }

    private Payment createPendingPayment(Long orderId, Long amount, PaymentMethod paymentMethod, String clientRequestId) {
        Long paymentId = paymentRepository.generateNextId();
        // 도메인 모델의 정적 팩토리 메서드 사용
        return Payment.createPending(paymentId, orderId, amount, paymentMethod, clientRequestId);
    }

    private void executePayment(Order order, Payment payment, PaymentMethod method) {
        log.info("[Payment] PG 결제 실행 시작 - orderId: {}, amount: {}, method: {}",
                order.getId(), order.getFinalAmount(), method);

        try {
            Map<String, Object> pgResponse = callPaymentGateway(order.getId(), order.getFinalAmount(), method);
            boolean success = (boolean) pgResponse.get("success");

            if (success) {
                handlePaymentSuccess(order, payment, pgResponse);
            } else {
                handlePaymentFailure(order, payment, pgResponse);
            }
        } catch (RestClientException e) {
            log.error("[Payment] PG 통신 오류 - orderId: {}, error: {}", order.getId(), e.getMessage());
            handlePaymentException(order, payment, e);
            throw new BusinessException(PaymentErrorCode.PG_COMMUNICATION_FAILED);
        } catch (Exception e) {
            log.error("[Payment] 결제 처리 중 예외 발생 - orderId: {}, error: {}", order.getId(), e.getMessage(), e);
            handlePaymentException(order, payment, e);
            throw new BusinessException(PaymentErrorCode.PAYMENT_FAILED);
        }
    }

    private void handlePaymentSuccess(Order order, Payment payment, Map<String, Object> response) {
        String transactionId = (String) response.get("transactionId");
        log.debug("[Payment] PG 성공 응답 처리 - orderId: {}, transactionId: {}", order.getId(), transactionId);
        
        Payment existingByTxId = paymentRepository.findByTransactionId(transactionId).orElse(null);
        if (existingByTxId != null) {
            log.warn("[Payment] 중복 transactionId 감지 - transactionId: {}, 기존 paymentId: {}, 현재 paymentId: {}",
                    transactionId, existingByTxId.getId(), payment.getId());
            payment.markAsFailed("이미 동일한 transactionId로 결제가 완료되었습니다.");
            paymentRepository.save(payment);
            return;
        }

        payment.markAsSuccess(transactionId);
        paymentRepository.save(payment);

        orderService.completePayment(order.getId());

        log.info("[Payment] 결제 성공 처리 완료 - paymentId: {}, orderId: {}, transactionId: {}",
                payment.getId(), order.getId(), transactionId);
    }

    private void handlePaymentFailure(Order order, Payment payment, Map<String, Object> response) {
        String failReason = (String) response.get("message");
        log.warn("[Payment] PG 실패 응답 처리 - orderId: {}, reason: {}", order.getId(), failReason);

        payment.markAsFailed(failReason);
        paymentRepository.save(payment);

        orderService.cancelOrder(order.getId(), "결제 실패: " + failReason);

        log.info("[Payment] 결제 실패 처리 완료 - paymentId: {}, orderId: {}", payment.getId(), order.getId());
    }

    private void handlePaymentException(Order order, Payment payment, Exception e) {
        String errorMessage = "결제 게이트웨이 오류: " + e.getMessage();
        log.error("[Payment] 결제 예외 처리 시작 - orderId: {}, paymentId: {}, error: {}",
                order.getId(), payment.getId(), e.getMessage());

        payment.markAsFailed(errorMessage);
        paymentRepository.save(payment);

        orderService.cancelOrder(order.getId(), "결제 처리 중 오류 발생");

        log.info("[Payment] 결제 예외 처리 완료 - paymentId: {}, status: FAILED", payment.getId());
    }

    private Map<String, Object> callPaymentGateway(Long orderId, Long amount, PaymentMethod method) {
        Map<String, Object> request = Map.of(
                "orderId", orderId,
                "amount", amount,
                "paymentMethod", method.name()
        );

        return restClient.post()
                .uri(mockPaymentUrl)
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.of(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionId(),
                payment.getFailReason(),
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getCreatedAt()
        );
    }
}
