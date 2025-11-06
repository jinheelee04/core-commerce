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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    // 쿠폰별 ReentrantLock (동시성 제어용, 공정한 락)
    private final Map<Long, ReentrantLock> couponLocks = new ConcurrentHashMap<>();
    // 쿠폰별 발급 수량 추적
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
        ReentrantLock lock = couponLocks.computeIfAbsent(couponId, id -> new ReentrantLock(true)); // 공정한 락

        lock.lock();
        try {
            Coupon coupon = findCouponById(couponId);
            validateCouponIssuable(coupon);

            int currentIssued = issuedCount.getOrDefault(couponId, 0);
            if (currentIssued >= coupon.getTotalQuantity()) {
                throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }

            if (userCouponRepository.findByCouponIdAndUserId(couponId, userId).isPresent()) {
                log.warn("[Coupon] 중복 발급 시도 - userId: {}, couponId: {}", userId, couponId);
                throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
            }

            issuedCount.put(couponId, currentIssued + 1);

            // Coupon 객체의 remainingQuantity도 업데이트 (조회용)
            coupon.issue();
            couponRepository.save(coupon);

            Long userCouponId = userCouponRepository.generateNextId();
            UserCoupon userCoupon = UserCoupon.issue(userCouponId, couponId, userId, coupon.getEndsAt());
            UserCoupon savedCoupon = userCouponRepository.save(userCoupon);

            log.info("[Coupon] 발급 성공 - userId: {}, couponId: {}, issuedCount: {}/{}",
                    userId, couponId, currentIssued + 1, coupon.getTotalQuantity());

            return toUserCouponResponse(savedCoupon);
        } finally {
            lock.unlock();
        }
    }

    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);
        validateCouponUsable(userCoupon);

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

            // Coupon 객체의 remainingQuantity도 복구 (조회용)
            Coupon coupon = findCouponById(couponId);
            coupon.cancelIssue();
            couponRepository.save(coupon);

            log.info("[Coupon] 사용 취소 - userCouponId: {}, couponId: {}, issuedCount: {}",
                    userCouponId, couponId, currentIssued - 1);
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

    private void validateDuplicateIssuance(Long userId, Long couponId) {
        if (userCouponRepository.findByCouponIdAndUserId(couponId, userId).isPresent()) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    private void validateCouponIssuable(Coupon coupon) {
        if (!coupon.isIssuable()) {
            if (coupon.getRemainingQuantity() <= 0) {
                throw new BusinessException(CouponErrorCode.COUPON_OUT_OF_STOCK);
            }

            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }
    }

    private void validateCouponUsable(UserCoupon userCoupon) {
        if (!userCoupon.isUsable()) {
            if (userCoupon.getIsUsed()) {
                throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
            }
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }
    }

    private void decreaseCouponQuantity(Coupon coupon) {
        coupon.issue();
        couponRepository.save(coupon);
    }

    private UserCoupon createUserCoupon(Long userId, Coupon coupon) {
        Long userCouponId = userCouponRepository.generateNextId();
        UserCoupon userCoupon = UserCoupon.issue(userCouponId, coupon.getId(), userId, coupon.getEndsAt());
        return userCouponRepository.save(userCoupon);
    }

    private void restoreCouponQuantity(Long couponId) {
        Coupon coupon = findCouponById(couponId);
        coupon.cancelIssue();
        couponRepository.save(coupon);
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
