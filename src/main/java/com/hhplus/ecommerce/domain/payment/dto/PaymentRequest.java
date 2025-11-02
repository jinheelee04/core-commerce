package com.hhplus.ecommerce.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 요청")
public record PaymentRequest(
        @Schema(description = "주문 ID", example = "456", required = true)
        Long orderId,

        @Schema(description = "결제 수단", example = "CARD", required = true, allowableValues = {"CARD", "VIRTUAL_ACCOUNT", "PHONE"})
        String paymentMethod,

        @Schema(description = "결제 금액 (원)", example = "4500000", required = true)
        Long amount
) {
}
