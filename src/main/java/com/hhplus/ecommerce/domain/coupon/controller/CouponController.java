package com.hhplus.ecommerce.domain.coupon.controller;

import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.global.common.dto.ApiResponse;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class CouponController {

    @GetMapping("/coupons/available")
    public ApiResponse<List<Map<String, Object>>> getCoupons( @RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(required = false) Long orderAmount) {
        List<Map<String, Object>> activeCoupons = new ArrayList<>();
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

            if(!isIssued) activeCoupons.add(coupon);
        }

        return ApiResponse.of(activeCoupons);
    }

    @PostMapping("/users/me/coupons")
    public ApiResponse<Map<String, Object>> issueCoupon(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> request
    ) {
        Long couponId = Long.parseLong(request.get("couponId").toString());
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
            return ApiResponse.of(Map.ofEntries(
                        Map.entry("userCouponId", userCouponId),
                        Map.entry("couponId", couponId),
                        Map.entry("userId", userId),
                        Map.entry("code",  coupon.get("code")),
                        Map.entry("name",  coupon.get("name")),
                        Map.entry("discountType", coupon.get("discountType")),
                        Map.entry("discountValue", coupon.get("discountValue")),
                        Map.entry("minOrderAmount", coupon.get("minOrderAmount")),
                        Map.entry("maxDiscountAmount", coupon.get("maxDiscountAmount")),
                        Map.entry("isUsed", false),
                        Map.entry("issuedAt", issuedAt),
                        Map.entry("expiresAt", expiresAt)
                    ));
        }
    }

    @GetMapping("/users/me/coupons")
    public ApiResponse<List<Map<String, Object>>> getMyCoupons(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Boolean isUsed
    ) {
        List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.getOrDefault(userId, new ArrayList<>());
        List<Map<String, Object>> result = new ArrayList<>();

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
                Map<String, Object> enriched = new HashMap<>(uc);
                enriched.put("userCouponId", uc.get("userCouponId"));
                enriched.put("couponId", couponId);
                enriched.put("userId", userId);
                enriched.put("code", coupon.get("code"));
                enriched.put("name", coupon.get("name"));
                enriched.put("discountType", coupon.get("discountType"));
                enriched.put("discountValue", coupon.get("discountValue"));
                enriched.put("minOrderAmount", coupon.get("minOrderAmount"));
                enriched.put("isUsed", used);
                enriched.put("issuedAt", uc.get("issuedAt"));
                enriched.put("expiresAt", uc.get("expiresAt"));
                result.add(enriched);
            }
        }

        return ApiResponse.of(result);
    }
}
