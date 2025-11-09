package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @NotEmpty(message = "주문할 상품이 최소 1개 이상이어야 합니다")
        @Schema(description = "주문할 장바구니 항목 ID 목록", example = "[1, 2, 3]", required = true)
        List<Long> cartItemIds,

        @Schema(description = "사용할 쿠폰 ID (선택)", example = "10")
        Long couponId,

        @NotBlank(message = "배송지 주소는 필수입니다")
        @Size(max = 500, message = "배송지 주소는 최대 500자까지 입력 가능합니다")
        @Schema(description = "배송지 주소", example = "서울시 강남구 테헤란로 123", required = true)
        String deliveryAddress,

        @Size(max = 200, message = "배송 메모는 최대 200자까지 입력 가능합니다")
        @Schema(description = "배송 메모", example = "문 앞에 놔주세요")
        String deliveryMemo
) {
}
