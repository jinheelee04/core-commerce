package com.hhplus.ecommerce.domain.payment.controller;

import com.hhplus.ecommerce.domain.payment.dto.PaymentRequest;
import com.hhplus.ecommerce.domain.payment.dto.PaymentResponse;
import com.hhplus.ecommerce.domain.payment.model.PaymentMethod;
import com.hhplus.ecommerce.domain.payment.service.PaymentService;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.constants.HttpHeaders;
import com.hhplus.ecommerce.global.constants.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 API", description = "결제 처리 및 조회 관련 API")
@SecurityRequirement(name = SecurityConstants.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "결제 요청",
            description = "주문에 대한 결제를 처리합니다. X-Idempotency-Key 헤더로 중복 결제를 방지할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 성공 또는 중복 요청(멱등성 보장)"),
            @ApiResponse(responseCode = "400", description = "주문 상태 오류 또는 금액 불일치"),
            @ApiResponse(responseCode = "408", description = "결제 타임아웃")
    })
    @PostMapping
    public ResponseEntity<CommonResponse<PaymentResponse>> processPayment(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "멱등성 키 (중복 결제 방지용)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.paymentMethod());
        PaymentResponse response = paymentService.processPayment(userId, request.orderId(), paymentMethod, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(response));
    }

    @Operation(summary = "결제 상세 조회", description = "결제 ID로 결제 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<CommonResponse<PaymentResponse>> getPayment(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "결제 ID", example = "789", required = true)
            @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "주문별 결제 조회", description = "주문 ID로 결제 정보를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "결제 내역 없음")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<CommonResponse<PaymentResponse>> getPaymentByOrder(
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.X_USER_ID) Long userId,
            @Parameter(description = "주문 ID", example = "456", required = true)
            @PathVariable Long orderId
    ) {
        PaymentResponse response = paymentService.getPaymentByOrderId(userId, orderId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
