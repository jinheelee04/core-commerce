package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 취소 응답")
public record CancelOrderResponse(
        @Schema(description = "주문 ID", example = "456")
        Long orderId,

        @Schema(description = "주문 번호", example = "ORD-20250115-001")
        String orderNumber,

        @Schema(description = "주문 상태", example = "CANCELLED")
        String status,

        @Schema(description = "취소 사유", example = "단순 변심")
        String cancelReason,

        @Schema(description = "취소 시간", example = "2025-01-15T11:00:00")
        String cancelledAt
) {
    public static CancelOrderResponse of(Long orderId, String orderNumber, String status,
                                          String cancelReason, java.time.LocalDateTime cancelledAt) {
        return new CancelOrderResponse(
                orderId,
                orderNumber,
                status,
                cancelReason,
                cancelledAt != null ? cancelledAt.toString() : null
        );
    }
}
