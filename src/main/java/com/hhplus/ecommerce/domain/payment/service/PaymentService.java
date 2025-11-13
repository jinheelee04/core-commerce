package com.hhplus.ecommerce.domain.payment.service;

import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.entity.OrderStatus;
import com.hhplus.ecommerce.domain.order.service.OrderService;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.entity.Payment;
import com.hhplus.ecommerce.domain.payment.event.PaymentCompletedEvent;
import com.hhplus.ecommerce.domain.payment.event.PaymentFailedEvent;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import com.hhplus.ecommerce.domain.payment.repository.PaymentRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import com.hhplus.ecommerce.global.exception.DomainExceptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final CouponService couponService;
    private final RestClient restClient;
    private final ApplicationEventPublisher eventPublisher;
    private final DomainExceptionMapper exceptionMapper;

    @Value("${mock.payment.url}")
    private String mockPaymentUrl;

    public Payment findPaymentById(Long paymentId){
        return  paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public PaymentResponse processPayment(Long userId, Long orderId, Payment.PaymentMethod paymentMethod, String clientRequestId) {
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

        executePayment(order, payment, paymentMethod);

        log.info("[Payment] 결제 처리 완료 - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
        return toPaymentResponse(payment);
    }

    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentById(paymentId);
        validateOrderOwnership(userId, payment.getOrderId(), "결제 조회");

        return toPaymentResponse(payment);
    }

    public PaymentResponse getPaymentByOrderId(Long userId, Long orderId) {
        validateOrderOwnership(userId, orderId, "결제 조회");
        Payment payment = paymentRepository.findByOrderIdWithOrder(orderId)
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
            throw exceptionMapper.mapToPaymentException(e, context);
        }
    }

    private Order validateOrderForPayment(Long userId, Long orderId) {
        Order order;
        try {
            order = orderService.requireOrderOwnedByUser(userId, orderId);
        } catch (BusinessException e) {
            throw exceptionMapper.mapToPaymentException(e, "결제 처리");
        }

        if (order == null) {
            log.error("[Payment] 주문 조회 결과가 null - orderId: {}", orderId);
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[Payment] 유효하지 않은 주문 상태 - orderId: {}, status: {}", orderId, order.getStatus());
            throw new BusinessException(PaymentErrorCode.INVALID_ORDER_STATUS);
        }

        return order;
    }


    private Payment checkExistingSuccessfulPayment(Long orderId) {
        Payment existingPayment = paymentRepository.findByOrderIdWithOrder(orderId).orElse(null);
        if (existingPayment != null && existingPayment.isSuccess()) {
            return existingPayment;
        }
        return null;
    }

    private Payment createPendingPayment(Long orderId, Long amount, Payment.PaymentMethod paymentMethod, String clientRequestId) {
        Order order = orderService.findOrderById(orderId);
        return new Payment(order, amount, paymentMethod, clientRequestId);
    }

    private void executePayment(Order order, Payment payment, Payment.PaymentMethod method) {
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

        eventPublisher.publishEvent(PaymentCompletedEvent.of(order.getId(), payment.getId(), transactionId));

        log.info("[Payment] 결제 성공 처리 완료 - paymentId: {}, orderId: {}, transactionId: {}",
                payment.getId(), order.getId(), transactionId);
    }

    private void handlePaymentFailure(Order order, Payment payment, Map<String, Object> response) {
        String failReason = (String) response.get("message");
        log.warn("[Payment] PG 실패 응답 처리 - orderId: {}, reason: {}", order.getId(), failReason);

        payment.markAsFailed(failReason);
        paymentRepository.save(payment);

        eventPublisher.publishEvent(PaymentFailedEvent.of(order.getId(), payment.getId(), "결제 실패: " + failReason));

        log.info("[Payment] 결제 실패 처리 완료 - paymentId: {}, orderId: {}", payment.getId(), order.getId());
    }

    private void handlePaymentException(Order order, Payment payment, Exception e) {
        String errorMessage = "결제 게이트웨이 오류: " + e.getMessage();
        log.error("[Payment] 결제 예외 처리 시작 - orderId: {}, paymentId: {}, error: {}",
                order.getId(), payment.getId(), e.getMessage());

        payment.markAsFailed(errorMessage);
        paymentRepository.save(payment);

        eventPublisher.publishEvent(PaymentFailedEvent.of(order.getId(), payment.getId(), "결제 처리 중 오류 발생"));

        log.info("[Payment] 결제 예외 처리 완료 - paymentId: {}, status: FAILED", payment.getId());
    }

    private Map<String, Object> callPaymentGateway(Long orderId, Long amount, Payment.PaymentMethod method) {
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
        PaymentResponse.PaymentCouponInfo couponInfo = null;

        try {
            Order order = orderService.findOrderById(payment.getOrderId());
            if (order.getUserCouponId() != null) {
                couponInfo = getCouponInfo(order.getUserCouponId(), order.getDiscountAmount());
            }
        } catch (Exception e) {
            log.warn("[Payment] 쿠폰 정보 조회 실패 - paymentId: {}, orderId: {}, error: {}",
                    payment.getId(), payment.getOrderId(), e.getMessage());
        }

        return PaymentResponse.of(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionId(),
                payment.getFailReason(),
                couponInfo,
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getCreatedAt()
        );
    }

    private PaymentResponse.PaymentCouponInfo getCouponInfo(Long userCouponId, Long discountAmount) {
        try {
            UserCoupon userCoupon = couponService.findUserCouponById(userCouponId);
            Coupon coupon = couponService.findCouponById(userCoupon.getCouponId());
            return PaymentResponse.PaymentCouponInfo.of(
                    coupon.getId(),
                    coupon.getName(),
                    discountAmount
            );
        } catch (Exception e) {
            log.warn("[Payment] 쿠폰 정보 조회 실패 - userCouponId: {}, error: {}", userCouponId, e.getMessage());
            return null;
        }
    }
}
