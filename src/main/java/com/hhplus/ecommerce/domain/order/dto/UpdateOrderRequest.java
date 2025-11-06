package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "주문 취소 요청")
public record UpdateOrderRequest(
        @Schema(description = "변경할 주문 상태 (CANCELLED만 가능)", example = "CANCELLED", required = true, allowableValues = {"CANCELLED"})
        String status,

        @Size(max = 500, message = "취소 사유는 최대 500자까지 입력 가능합니다")
        @Schema(description = "취소 사유", example = "단순 변심")
        String cancelReason
) {
}
