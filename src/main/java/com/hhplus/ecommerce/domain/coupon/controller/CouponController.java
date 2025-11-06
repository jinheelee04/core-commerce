package com.hhplus.ecommerce.domain.coupon.controller;

import com.hhplus.ecommerce.domain.coupon.dto.CouponResponse;
import com.hhplus.ecommerce.domain.coupon.dto.IssueCouponRequest;
import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "쿠폰 API", description = "쿠폰 발급 및 조회 관련 API")
@SecurityRequirement(name = "X-User-Id")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(
            summary = "발급 가능한 쿠폰 목록 조회",
            description = "현재 발급 가능한 활성 상태의 쿠폰 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/coupons/available")
    public ResponseEntity<CommonResponse<List<CouponResponse>>> getAvailableCoupons(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId
    ) {
        List<CouponResponse> responses = couponService.getAvailableCoupons();
        return ResponseEntity.ok(CommonResponse.of(responses));
    }

    @Operation(
            summary = "쿠폰 발급",
            description = "선착순으로 쿠폰을 발급받습니다. 1인 1매 제한이 적용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "발급 불가능한 쿠폰 (만료 또는 비활성)"),
            @ApiResponse(responseCode = "409", description = "중복 발급 또는 수량 소진"),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @PostMapping("/users/me/coupons")
    public ResponseEntity<CommonResponse<UserCouponResponse>> issueCoupon(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody IssueCouponRequest request
    ) {
        UserCouponResponse response = couponService.issueCoupon(userId, request.couponId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(
            summary = "보유 쿠폰 목록 조회",
            description = "사용자가 보유한 쿠폰 목록을 조회합니다. isUsed 파라미터로 사용/미사용 쿠폰을 필터링할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/me/coupons")
    public ResponseEntity<CommonResponse<List<UserCouponResponse>>> getUserCoupons(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "사용 여부 필터 (true: 사용됨, false: 미사용)", example = "false")
            @RequestParam(required = false) Boolean isUsed
    ) {
        List<UserCouponResponse> responses = isUsed != null && isUsed
                ? couponService.getUserCoupons(userId).stream()
                        .filter(UserCouponResponse::isUsed)
                        .collect(Collectors.toList())
                : isUsed != null && !isUsed
                        ? couponService.getUnusedUserCoupons(userId)
                        : couponService.getUserCoupons(userId);

        return ResponseEntity.ok(CommonResponse.of(responses));
    }

    @Operation(
            summary = "쿠폰 상세 조회",
            description = "쿠폰 ID로 쿠폰 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @GetMapping("/coupons/{couponId}")
    public ResponseEntity<CommonResponse<CouponResponse>> getCoupon(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "쿠폰 ID", example = "1", required = true)
            @PathVariable Long couponId
    ) {
        CouponResponse response = couponService.getCoupon(couponId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}