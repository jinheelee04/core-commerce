package com.hhplus.ecommerce.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 요청")
public record PaymentRequest(
        @NotNull(message = "주문 ID는 필수입니다")
        @Schema(description = "주문 ID", example = "456", required = true)
        Long orderId,

        @NotBlank(message = "결제 수단은 필수입니다")
        @Schema(description = "결제 수단", example = "CARD", required = true, allowableValues = {"CARD", "VIRTUAL_ACCOUNT", "PHONE"})
        String paymentMethod,

        @NotNull(message = "결제 금액은 필수입니다")
        @Min(value = 0, message = "결제 금액은 0원 이상이어야 합니다")
        @Schema(description = "결제 금액 (원)", example = "4500000", required = true)
        Long amount
) {
}
