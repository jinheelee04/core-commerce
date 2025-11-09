package com.hhplus.ecommerce.domain.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주문 상세 응답")
public record OrderResponse(
        @Schema(description = "주문 ID", example = "456")
        Long orderId,

        @Schema(description = "주문 번호", example = "ORD-20250115-001")
        String orderNumber,

        @Schema(description = "사용자 ID", example = "123")
        Long userId,

        @Schema(description = "주문 상태", example = "PENDING", allowableValues = {"PENDING", "PAID", "CANCELLED"})
        String status,

        @Schema(description = "주문 항목 목록")
        List<OrderItemResponse> items,

        @Schema(description = "가격 정보")
        OrderPricingResponse pricing,

        @Schema(description = "적용된 쿠폰 정보")
        OrderCouponResponse coupon,

        @Schema(description = "배송지 주소", example = "서울시 강남구 테헤란로 123")
        String deliveryAddress,

        @Schema(description = "배송 메모", example = "문 앞에 놔주세요")
        String deliveryMemo,

        @Schema(description = "주문 만료 시간 (10분)", example = "2025-01-15T10:45:00")
        String expiresAt,

        @Schema(description = "결제 완료 시간", example = "2025-01-15T10:35:00")
        String paidAt,

        @Schema(description = "취소 시간", example = "2025-01-15T11:00:00")
        String cancelledAt,

        @Schema(description = "취소 사유", example = "단순 변심")
        String cancelReason,

        @Schema(description = "주문 생성 시간", example = "2025-01-15T10:30:00")
        String createdAt
) {
    public static OrderResponse of(Long orderId, String orderNumber, Long userId, String status,
                                     List<OrderItemResponse> items, OrderPricingResponse pricing,
                                     OrderCouponResponse coupon, String deliveryAddress, String deliveryMemo,
                                     java.time.LocalDateTime expiresAt, java.time.LocalDateTime paidAt,
                                     java.time.LocalDateTime cancelledAt, String cancelReason,
                                     java.time.LocalDateTime createdAt) {
        return new OrderResponse(
                orderId,
                orderNumber,
                userId,
                status,
                items,
                pricing,
                coupon,
                deliveryAddress,
                deliveryMemo,
                expiresAt != null ? expiresAt.toString() : null,
                paidAt != null ? paidAt.toString() : null,
                cancelledAt != null ? cancelledAt.toString() : null,
                cancelReason,
                createdAt != null ? createdAt.toString() : null
        );
    }
}
