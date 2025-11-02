package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 취소 요청")
public record UpdateOrderRequest(
        @Schema(description = "변경할 주문 상태 (CANCELLED만 가능)", example = "CANCELLED", required = true, allowableValues = {"CANCELLED"})
        String status,

        @Schema(description = "취소 사유", example = "단순 변심")
        String cancelReason
) {
}
