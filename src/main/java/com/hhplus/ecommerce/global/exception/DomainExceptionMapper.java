package com.hhplus.ecommerce.global.exception;

import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.payment.exception.PaymentErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도메인 간 예외 매핑을 중앙에서 관리하는 클래스
 *
 * 각 도메인에서 발생한 예외를 다른 도메인의 컨텍스트에 맞게 변환
 * 예: Order 도메인 예외 → Payment 도메인 예외로 변환
 */
@Slf4j
@Component
public class DomainExceptionMapper {

    /**
     * Order 도메인 예외를 Payment 도메인 예외로 매핑
     *
     * @param e Order 도메인에서 발생한 예외
     * @param context 예외 발생 컨텍스트 (로깅용)
     * @return Payment 도메인에 적합한 예외
     */
    public BusinessException mapToPaymentException(BusinessException e, String context) {
        ErrorCode errorCode = e.getErrorCode();

        if (errorCode == OrderErrorCode.ORDER_NOT_FOUND) {
            log.warn("[ExceptionMapper] {} - Order not found, mapping to Payment not found", context);
            return new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        if (errorCode == OrderErrorCode.ORDER_ACCESS_DENIED) {
            log.warn("[ExceptionMapper] {} - Order access denied, mapping to Payment not allowed", context);
            return new BusinessException(PaymentErrorCode.PAYMENT_NOT_ALLOWED);
        }

        log.debug("[ExceptionMapper] {} - No mapping rule for {}, returning original exception",
                context, errorCode);
        return e;
    }

    /**
     * 다른 도메인 예외를 Order 도메인 예외로 매핑 (필요 시 확장)
     */
    public BusinessException mapToOrderException(BusinessException e, String context) {
        // 향후 필요 시 구현
        return e;
    }

    /**
     * 범용 매핑 메서드 (향후 확장)
     *
     * @param e 원본 예외
     * @param targetDomain 목표 도메인 (예: "payment", "order")
     * @param context 컨텍스트
     * @return 매핑된 예외
     */
    public BusinessException mapException(BusinessException e, String targetDomain, String context) {
        return switch (targetDomain.toLowerCase()) {
            case "payment" -> mapToPaymentException(e, context);
            case "order" -> mapToOrderException(e, context);
            default -> {
                log.debug("[ExceptionMapper] Unknown target domain: {}, returning original exception", targetDomain);
                yield e;
            }
        };
    }
}
