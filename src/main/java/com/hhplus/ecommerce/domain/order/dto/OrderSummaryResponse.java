package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 요약 응답")
public record OrderSummaryResponse(
        @Schema(description = "주문 ID", example = "456")
        Long orderId,

        @Schema(description = "주문 번호", example = "ORD-20250115-001")
        String orderNumber,

        @Schema(description = "주문 상태", example = "PAID", allowableValues = {"PENDING", "PAID", "CANCELLED"})
        String status,

        @Schema(description = "주문 상품 수", example = "3")
        Integer itemCount,

        @Schema(description = "총 결제 금액 (원)", example = "90000")
        Long totalAmount,

        @Schema(description = "주문 생성 시간", example = "2025-01-15T10:30:00")
        String createdAt
) {
}
