package com.hhplus.ecommerce.domain.coupon.service;

import com.hhplus.ecommerce.domain.coupon.dto.CouponResponse;
import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.entity.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.exception.OrderErrorCode;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.domain.user.repository.UserRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public List<CouponResponse> getAvailableCoupons() {
        List<Coupon> coupons = couponRepository.findIssuableCoupons(
                CouponStatus.ACTIVE,
                LocalDateTime.now()
        );
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

    @Transactional
    public UserCouponResponse issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.USER_NOT_FOUND));

        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
        }

        coupon.issue();

        UserCoupon userCoupon = new UserCoupon(coupon, user, coupon.getEndsAt());
        UserCoupon savedCoupon = userCouponRepository.save(userCoupon);

        return toUserCouponResponse(savedCoupon);
    }

    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        userCoupon.use(order);
    }

    @Transactional
    public void cancelCouponUse(Long userCouponId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        Coupon coupon = userCoupon.getCoupon();

        Coupon lockedCoupon = couponRepository.findByIdWithLock(coupon.getId())
                .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));

        userCoupon.cancel();
        lockedCoupon.restore();
    }

    @Transactional
    public void reserveCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = findUserCouponById(userCouponId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        userCoupon.use(order);
    }

    @Transactional
    public void confirmCouponReservation(Long userCouponId) {
        // 이미 useCoupon/reserveCoupon에서 사용 처리가 완료되어 있으므로 별도 작업 불필요
        // 필요시 추가 로직 구현
    }

    @Transactional
    public void releaseCouponReservation(Long userCouponId) {
        // 쿠폰 사용 취소와 동일
        cancelCouponUse(userCouponId);
    }

    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = findCouponById(couponId);
        return toCouponResponse(coupon);
    }

    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));
        return toCouponResponse(coupon);
    }

    public Coupon findCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    public UserCoupon findUserCouponById(Long userCouponId) {
        return userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_NOT_FOUND));
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
        Coupon coupon = userCoupon.getCoupon();
        return new UserCouponResponse(
                userCoupon.getId(),
                coupon.getId(),
                userCoupon.getUser().getId(),
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
