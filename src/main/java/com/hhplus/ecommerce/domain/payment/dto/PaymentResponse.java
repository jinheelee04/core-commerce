package com.hhplus.ecommerce.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID", example = "789")
        Long paymentId,

        @Schema(description = "주문 ID", example = "456")
        Long orderId,

        @Schema(description = "상품 금액 (원)", example = "100000")
        Long amount,

        @Schema(description = "할인 금액 (원)", example = "10000")
        Long discountAmount,

        @Schema(description = "최종 결제 금액 (원)", example = "90000")
        Long finalAmount,

        @Schema(description = "결제 수단", example = "CARD", allowableValues = {"CARD", "VIRTUAL_ACCOUNT", "PHONE"})
        String paymentMethod,

        @Schema(description = "결제 상태", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        String status,

        @Schema(description = "PG사 거래 ID", example = "TXN-20250115-12345")
        String transactionId,

        @Schema(description = "실패 사유", example = "한도 초과")
        String failReason,

        @Schema(description = "결제 성공 시간", example = "2025-01-15T10:35:00")
        String paidAt,

        @Schema(description = "결제 실패 시간", example = "2025-01-15T10:35:00")
        String failedAt,

        @Schema(description = "결제 생성 시간", example = "2025-01-15T10:35:00")
        String createdAt
) {
}
