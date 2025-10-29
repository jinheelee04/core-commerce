package com.hhplus.ecommerce.mock.coupon;

import com.hhplus.ecommerce.storage.InMemoryDataStore;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 쿠폰 관리 Controller
 */
@RestController
@RequestMapping("/api/v1/coupons")
public class MockCouponController {

    /**
     * 사용 가능한 쿠폰 목록 조회
     * GET /api/coupons
     */
    @GetMapping
    public Map<String, Object> getCoupons() {
        List<Map<String, Object>> activeCoupons = new ArrayList<>();

        for (Map<String, Object> coupon : InMemoryDataStore.COUPONS.values()) {
            if ("ACTIVE".equals(coupon.get("status"))) {
                int remaining = (int) coupon.get("remainingQuantity");
                if (remaining > 0) {
                    activeCoupons.add(coupon);
                }
            }
        }

        return Map.of(
            "coupons", activeCoupons,
            "total", activeCoupons.size()
        );
    }

    /**
     * 쿠폰 상세 조회
     * GET /api/coupons/{code}
     */
    @GetMapping("/{code}")
    public Map<String, Object> getCoupon(@PathVariable String code) {
        Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(code);
        if (coupon == null) {
            return Map.of("error", "쿠폰을 찾을 수 없습니다");
        }
        return coupon;
    }

    /**
     * 쿠폰 발급
     * POST /api/coupons/{code}/issue
     */
    @PostMapping("/{code}/issue")
    public Map<String, Object> issueCoupon(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String code
    ) {
        // 1. 쿠폰 확인
        Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(code);
        if (coupon == null) {
            return Map.of("error", "쿠폰을 찾을 수 없습니다");
        }

        if (!"ACTIVE".equals(coupon.get("status"))) {
            return Map.of("error", "발급할 수 없는 쿠폰입니다");
        }

        // 2. 잔여 수량 확인
        synchronized (coupon) {
            int remaining = (int) coupon.get("remainingQuantity");
            if (remaining <= 0) {
                return Map.of("error", "쿠폰이 모두 소진되었습니다");
            }

            // 3. 중복 발급 확인 (1인 1매 제한)
            List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.computeIfAbsent(
                userId, k -> new ArrayList<>()
            );

            for (Map<String, Object> uc : userCoupons) {
                if (code.equals(uc.get("code"))) {
                    return Map.of("error", "이미 발급받은 쿠폰입니다");
                }
            }

            // 4. 쿠폰 발급
            Long userCouponId = InMemoryDataStore.nextUserCouponId();
            Map<String, Object> userCoupon = new HashMap<>(Map.of(
                "userCouponId", userCouponId,
                "code", code,
                "couponName", coupon.get("name"),
                "userId", userId,
                "isUsed", false,
                "issuedAt", LocalDateTime.now().toString()
            ));

            userCoupons.add(userCoupon);

            // 잔여 수량 감소
            coupon.put("remainingQuantity", remaining - 1);

            return Map.of(
                "userCouponId", userCouponId,
                "code", code,
                "couponName", coupon.get("name"),
                "message", "쿠폰이 발급되었습니다"
            );
        }
    }

    /**
     * 내 쿠폰 목록 조회
     * GET /api/coupons/my
     */
    @GetMapping("/my")
    public Map<String, Object> getMyCoupons(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean includeUsed
    ) {
        List<Map<String, Object>> userCoupons = InMemoryDataStore.USER_COUPONS.getOrDefault(userId, new ArrayList<>());
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> uc : userCoupons) {
            boolean isUsed = (Boolean) uc.getOrDefault("isUsed", false);

            if (includeUsed || !isUsed) {
                // 쿠폰 상세 정보 추가
                String code = (String) uc.get("code");
                Map<String, Object> coupon = InMemoryDataStore.COUPONS.get(code);

                if (coupon != null) {
                    Map<String, Object> enriched = new HashMap<>(uc);
                    enriched.put("discountType", coupon.get("discountType"));
                    enriched.put("discountValue", coupon.get("discountValue"));
                    enriched.put("minOrderAmount", coupon.get("minOrderAmount"));
                    enriched.put("maxDiscountAmount", coupon.get("maxDiscountAmount"));
                    result.add(enriched);
                }
            }
        }

        return Map.of(
            "coupons", result,
            "total", result.size()
        );
    }
}
