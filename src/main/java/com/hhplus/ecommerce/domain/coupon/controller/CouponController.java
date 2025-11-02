package com.hhplus.ecommerce.domain.coupon.controller;

import com.hhplus.ecommerce.domain.coupon.dto.CouponResponse;
import com.hhplus.ecommerce.domain.coupon.dto.IssueCouponRequest;
import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.global.common.dto.CommonResponse;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "쿠폰 API", description = "쿠폰 발급 및 조회 관련 API")
@SecurityRequirement(name = "X-User-Id")
@RestController
@RequestMapping("/api/v1")
public class CouponController {

    @Operation(summary = "사용 가능한 쿠폰 조회", description = "발급 가능한 활성 쿠폰 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/coupons/available")
    public CommonResponse<List<CouponResponse>> getCoupons(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "주문 금액 (최소 금액 필터링용)", example = "50000")
            @RequestParam(required = false) Long orderAmount) {
        List<CouponResponse> activeCoupons = new ArrayList<>();
        List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.computeIfAbsent(
                userId, k -> new ArrayList<>()
        );
        for (Map<String, Object> coupon : InMemoryDataStore.COUPONS.values()) {
            if (!"ACTIVE".equals(coupon.get("status"))) continue;
            if(orderAmount != null){
                Long minOrderAmount = (Long) coupon.get("minOrderAmount");
                if(orderAmount < minOrderAmount) continue;
            }

            int remaining = ((Number) coupon.get("remainingQuantity")).intValue();
            if (remaining <= 0) continue;

            boolean isIssued = userCoupons.stream().anyMatch(uc -> uc.get("couponId").equals(coupon.get("couponId")));

            if(!isIssued) {
                activeCoupons.add(toCouponResponse(coupon));
            }
        }

        return CommonResponse.of(activeCoupons);
    }

    @Operation(summary = "쿠폰 발급 (선착순)", description = "선착순으로 쿠폰을 발급받습니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "발급 성공"),
            @ApiResponse(responseCode = "409", description = "중복 발급 또는 수량 소진"),
            @ApiResponse(responseCode = "400", description = "발급 기간 오류 또는 만료된 쿠폰")
    })
    @PostMapping("/users/me/coupons")
    public CommonResponse<UserCouponResponse> issueCoupon(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody IssueCouponRequest request
    ) {
        Long couponId = request.couponId();
        // 1. 쿠폰 확인
        Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(couponId);
        if (coupon == null) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
        }

        if (!"ACTIVE".equals(coupon.get("status"))) {
            throw new BusinessException(CouponErrorCode.COUPON_INACTIVE);
        }

        // 2. 잔여 수량 확인
        synchronized (coupon) {
            int remaining = (int) coupon.get("remainingQuantity");
            if (remaining <= 0) {
                throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }

            // 3. 중복 발급 확인 (1인 1매 제한)
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.computeIfAbsent(
                userId, k -> new ArrayList<>()
            );

            for (Map<String, Object> uc : userCoupons) {
                if (couponId.equals(uc.get("couponId"))) {
                    throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
                }
            }
            // 4. 쿠폰 만료 확인
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startsAt = LocalDateTime.parse((String) coupon.get("startsAt"));
            LocalDateTime endsAt = LocalDateTime.parse((String) coupon.get("endsAt"));
            if(now.isBefore(startsAt)){
                throw new BusinessException(CouponErrorCode.COUPON_NOT_STARTED);
            }

            if(now.isAfter(endsAt)){
                throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
            }


            // 5. 쿠폰 발급
            Long userCouponId = InMemoryDataStore.nextUserCouponId();
            String issuedAt = LocalDateTime.now().toString();
            String expiresAt = LocalDateTime.now().plusDays(30).toString();
            Map<String, Object> userCoupon = new HashMap<>(Map.of(
                    "userCouponId", userCouponId,
                    "couponId", couponId,
                    "userId" , userId,
                    "isUsed", false,
                    "issuedAt", issuedAt,
                    "expiresAt", expiresAt
            ));

            userCoupons.add(userCoupon);

            // 잔여 수량 감소
            coupon.put("remainingQuantity", remaining - 1);

            return CommonResponse.of(new UserCouponResponse(
                    userCouponId,
                    couponId,
                    userId,
                    (String) coupon.get("code"),
                    (String) coupon.get("name"),
                    (String) coupon.get("discountType"),
                    (Integer) coupon.get("discountValue"),
                    (Long) coupon.get("minOrderAmount"),
                    (Long) coupon.get("maxDiscountAmount"),
                    false,
                    issuedAt,
                    null,
                    expiresAt
            ));
        }
    }

    @Operation(summary = "보유 쿠폰 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/me/coupons")
    public CommonResponse<List<UserCouponResponse>> getMyCoupons(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "사용 여부 필터 (true: 사용됨, false: 미사용)", example = "false")
            @RequestParam(required = false) Boolean isUsed
    ) {
        List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.getOrDefault(userId, new ArrayList<>());
        List<UserCouponResponse> result = new ArrayList<>();

        for (Map<String, Object> uc : userCoupons) {
            boolean used = (Boolean) uc.getOrDefault("isUsed", false);

            // isUsed 파라미터가 제공되면 필터링
            if (isUsed != null && used != isUsed) {
                continue;
            }

            // 쿠폰 상세 정보 추가
            Long couponId = (Long) uc.get("couponId");
            Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(couponId);

            if (coupon != null) {
                result.add(new UserCouponResponse(
                        (Long) uc.get("userCouponId"),
                        couponId,
                        userId,
                        (String) coupon.get("code"),
                        (String) coupon.get("name"),
                        (String) coupon.get("discountType"),
                        (Integer) coupon.get("discountValue"),
                        (Long) coupon.get("minOrderAmount"),
                        (Long) coupon.get("maxDiscountAmount"),
                        used,
                        (String) uc.get("issuedAt"),
                        (String) uc.get("usedAt"),
                        (String) uc.get("expiresAt")
                ));
            }
        }

        return CommonResponse.of(result);
    }

    private CouponResponse toCouponResponse(Map<String, Object> coupon) {
        return new CouponResponse(
                (Long) coupon.get("couponId"),
                (String) coupon.get("code"),
                (String) coupon.get("name"),
                (String) coupon.get("description"),
                (String) coupon.get("discountType"),
                (Integer) coupon.get("discountValue"),
                (Long) coupon.get("minOrderAmount"),
                (Long) coupon.get("maxDiscountAmount"),
                (Integer) coupon.get("totalQuantity"),
                (Integer) coupon.get("remainingQuantity"),
                (String) coupon.get("startsAt"),
                (String) coupon.get("endsAt"),
                (String) coupon.get("status")
        );
    }
}
