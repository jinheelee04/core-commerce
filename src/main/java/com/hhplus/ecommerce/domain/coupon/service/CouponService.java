package com.hhplus.ecommerce.domain.coupon.service;

import com.hhplus.ecommerce.domain.coupon.dto.CouponResponse;
import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();
    private final Map<Long, Integer> issuedCount = new ConcurrentHashMap<>();

    public List<CouponResponse> getAvailableCoupons() {
        List<Coupon> coupons = couponRepository.findIssuableCoupons();
        return coupons.stream()
                .map(this::toCouponResponse)
                .toList();
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream()
                .map(this::toUserCouponResponse)
                .toList();
    }

    public List<UserCouponResponse> getUnusedUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndIsUsed(userId, false);
        return userCoupons.stream()
                .map(this::toUserCouponResponse)
                .toList();
    }

    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, id -> new ReentrantLock(true));

        lock.lock();
        try {
            Coupon coupon = findCouponById(couponId);

            int currentIssued = issuedCount.getOrDefault(couponId, 0);
            if (currentIssued >= coupon.getTotalQuantity()) {
                throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }

            if (userCouponRepository.findByCouponIdAndUserId(couponId, userId).isPresent()) {
                throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            }

            issuedCount.put(couponId, currentIssued + 1);

            coupon.issue();
            couponRepository.save(coupon);

            Long userCouponId = userCouponRepository.generateNextId();
            UserCoupon userCoupon = UserCoupon.issue(userCouponId, couponId, userId, coupon.getEndsAt());
            UserCoupon savedCoupon = userCouponRepository.save(userCoupon);

            return toUserCouponResponse(savedCoupon);
        } finally {
            lock.unlock();
        }
    }

    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        userCoupon.use(orderId);
        userCouponRepository.save(userCoupon);
    }

    public void cancelCouponUse(Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        Long couponId = userCoupon.getCouponId();

        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, id -> new ReentrantLock(true));

        lock.lock();
        try {
            userCoupon.cancelUse();
            userCouponRepository.save(userCoupon);

            int currentIssued = issuedCount.getOrDefault(couponId, 0);
            if (currentIssued > 0) {
                issuedCount.put(couponId, currentIssued - 1);
            }

            Coupon coupon = findCouponById(couponId);
            coupon.cancelIssue();
            couponRepository.save(coupon);

        } finally {
            lock.unlock();
        }
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = findCouponById(couponId);
        return toCouponResponse(coupon);
    }

    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> {
                    return new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
                });
        return toCouponResponse(coupon);
    }

    public Coupon getCouponEntity(Long couponId) {
        return findCouponById(couponId);
    }

    public UserCoupon getUserCouponEntity(Long userCouponId) {
        return findUserCouponById(userCouponId);
    }

    private Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> {
                    return new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
                });
    }

    private UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> {
                    return new BusinessException(CouponErrorCode.COUPON_NOT_FOUND);
                });
    }

    private CouponResponse toCouponResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity(),
                coupon.getStartsAt().toString(),
                coupon.getEndsAt().toString(),
                coupon.getStatus().name()
        );
    }

    private UserCouponResponse toUserCouponResponse(UserCoupon userCoupon) {
        Coupon coupon = findCouponById(userCoupon.getCouponId());
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCouponId(),
                userCoupon.getUserId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                userCoupon.getIsUsed(),
                userCoupon.getIssuedAt().toString(),
                userCoupon.getUsedAt() != null ? userCoupon.getUsedAt().toString() : null,
                userCoupon.getExpiresAt().toString()
        );
    }
}
