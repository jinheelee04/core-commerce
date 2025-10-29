package com.hhplus.ecommerce.mock.payment;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mock 결제 게이트웨이 Controller
 * 실제 PG사를 모방하여 80% 확률로 결제 성공을 시뮬레이션합니다.
 */
@RestController
@RequestMapping("/mock/api/v1/payments")
public class MockPaymentGatewayController {

    private final Random random = new Random();

    /**
     * Mock 결제 처리
     * POST /mock/api/v1/payments/process
     *
     * 80% 확률로 성공, 20% 확률로 실패
     */
    @PostMapping("/process")
    public Map<String, Object> processPayment(@RequestBody Map<String, Object> request) {
        Long orderId = ((Number) request.get("orderId")).longValue();
        Long amount = ((Number) request.get("amount")).longValue();
        String paymentMethod = (String) request.get("paymentMethod");

        // 80% 확률로 성공
        boolean isSuccess = random.nextInt(100) < 80;

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amount);
        response.put("paymentMethod", paymentMethod);

        if (isSuccess) {
            // 결제 성공
            response.put("status", "SUCCESS");
            response.put("transactionId", "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            response.put("paidAt", LocalDateTime.now().toString());
            response.put("message", "결제가 성공적으로 처리되었습니다");
        } else {
            // 결제 실패 (20% 확률)
            response.put("status", "FAILED");
            response.put("failReason", getRandomFailReason());
            response.put("failedAt", LocalDateTime.now().toString());
        }

        return response;
    }

    /**
     * 랜덤 실패 사유 반환
     */
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
