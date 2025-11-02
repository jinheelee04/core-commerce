package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @Schema(description = "주문할 장바구니 항목 ID 목록", example = "[1, 2, 3]", required = true)
        List<Long> cartItemIds,

        @Schema(description = "사용할 쿠폰 ID (선택)", example = "10")
        Long couponId,

        @Schema(description = "배송지 주소", example = "서울시 강남구 테헤란로 123", required = true)
        String deliveryAddress,

        @Schema(description = "배송 메모", example = "문 앞에 놔주세요")
        String deliveryMemo
) {
}
