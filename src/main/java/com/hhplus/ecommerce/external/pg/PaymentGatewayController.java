package com.hhplus.ecommerce.external.pg;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Hidden
@RestController
@RequestMapping("/mock/api/v1/payments")
public class PaymentGatewayController {

    private final Random random = new Random();
    private final Map<String, Map<String, Object>> processedPayments = new ConcurrentHashMap<>();

    @PostMapping("/process")
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> request) {
        Long orderId = ((Number) request.get("orderId")).longValue();
        Long amount = ((Number) request.get("amount")).longValue();
        String paymentMethod = (String) request.get("paymentMethod");

        log.info("[Mock PG] 결제 요청 수신 - orderId: {}, amount: {}, method: {}",
            orderId, amount, paymentMethod);

        String cacheKey = orderId + "_" + amount;
        if (processedPayments.containsKey(cacheKey)) {
            log.info("[Mock PG] 멱등성 - 기존 응답 반환: {}", cacheKey);
            return processedPayments.get(cacheKey);
        }

        boolean isSuccess = random.nextInt(100) < 80;

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amount);
        response.put("paymentMethod", paymentMethod);
        response.put("success", isSuccess);

        if (isSuccess) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            response.put("transactionId", transactionId);
            response.put("paidAt", LocalDateTime.now().toString());
            response.put("message", "결제가 성공적으로 처리되었습니다");
            log.info("[Mock PG] 결제 성공 - orderId: {}, transactionId: {}", orderId, transactionId);
        } else {
            String failReason = getRandomFailReason();
            response.put("message", failReason);
            response.put("failedAt", LocalDateTime.now().toString());
            log.warn("[Mock PG] 결제 실패 - orderId: {}, reason: {}", orderId, failReason);
        }

        processedPayments.put(cacheKey, response);
        return response;
    }

    private String getRandomFailReason() {
        String[] failReasons = {
            "카드 한도 초과",
            "잔액 부족",
            "카드 정보 불일치",
            "은행 시스템 오류",
            "승인 거부",
            "카드 사용 정지"
        };
        return failReasons[random.nextInt(failReasons.length)];
    }
}
