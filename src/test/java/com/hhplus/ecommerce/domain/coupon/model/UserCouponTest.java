package com.hhplus.ecommerce.domain.coupon.model;

import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserCoupon 도메인 모델 테스트")
class UserCouponTest {

    @Test
    @DisplayName("사용자 쿠폰 발급 시 isUsed는 false이고 발급 시간이 설정된다")
    void issue() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        UserCoupon userCoupon = UserCoupon.issue(1L, 100L, 200L, expiresAt);

        assertThat(userCoupon.getId()).isEqualTo(1L);
        assertThat(userCoupon.getCouponId()).isEqualTo(100L);
        assertThat(userCoupon.getUserId()).isEqualTo(200L);
        assertThat(userCoupon.getIsUsed()).isFalse();
        assertThat(userCoupon.getIssuedAt()).isNotNull();
        assertThat(userCoupon.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 - 미사용이고 만료 전인 경우")
    void isUsable_True() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));

        assertThat(userCoupon.isUsable()).isTrue();
    }

    @Test
    @DisplayName("사용 불가능한 쿠폰 - 이미 사용된 경우")
    void isUsable_False_AlreadyUsed() {
        UserCoupon userCoupon = createUserCoupon(true, LocalDateTime.now().plusDays(30));

        assertThat(userCoupon.isUsable()).isFalse();
    }

    @Test
    @DisplayName("사용 불가능한 쿠폰 - 만료된 경우")
    void isUsable_False_Expired() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().minusDays(1));

        assertThat(userCoupon.isUsable()).isFalse();
    }

    @Test
    @DisplayName("쿠폰이 만료되었는지 확인")
    void isExpired() {
        UserCoupon expiredCoupon = createUserCoupon(false, LocalDateTime.now().minusDays(1));
        UserCoupon validCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));

        assertThat(expiredCoupon.isExpired()).isTrue();
        assertThat(validCoupon.isExpired()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 예약 성공 - orderId가 설정된다")
    void reserve_Success() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));
        Long orderId = 500L;

        userCoupon.reserve(orderId);

        assertThat(userCoupon.getIsUsed()).isFalse();
        assertThat(userCoupon.getOrderId()).isEqualTo(orderId);
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 예약 확정 성공 - isUsed가 true로 변경되고 usedAt이 설정된다")
    void confirmReservation_Success() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));
        Long orderId = 500L;

        userCoupon.reserve(orderId);
        userCoupon.confirmReservation();

        assertThat(userCoupon.getIsUsed()).isTrue();
        assertThat(userCoupon.getOrderId()).isEqualTo(orderId);
        assertThat(userCoupon.getUsedAt()).isNotNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 예약 실패 - 이미 사용된 쿠폰")
    void reserve_Fail_AlreadyUsed() {
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .couponId(100L)
                .userId(200L)
                .orderId(999L)
                .isUsed(true)
                .issuedAt(now.minusDays(5))
                .usedAt(now.minusDays(2))
                .expiresAt(now.plusDays(30))
                .updatedAt(now)
                .build();
        Long orderId = 500L;

        assertThatThrownBy(() -> userCoupon.reserve(orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_USED);
    }

    @Test
    @DisplayName("쿠폰 예약 실패 - 이미 예약된 쿠폰")
    void reserve_Fail_AlreadyReserved() {
        UserCoupon userCoupon = createUserCouponWithOrder(false, 999L, LocalDateTime.now().plusDays(30));
        Long orderId = 500L;

        assertThatThrownBy(() -> userCoupon.reserve(orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_RESERVED);
    }

    @Test
    @DisplayName("쿠폰 예약 실패 - 만료된 쿠폰")
    void reserve_Fail_Expired() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().minusDays(1));
        Long orderId = 500L;

        assertThatThrownBy(() -> userCoupon.reserve(orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_EXPIRED);
    }

    @Test
    @DisplayName("쿠폰 예약 해제 성공 - orderId가 null이 된다")
    void releaseReservation_Success() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));
        Long orderId = 500L;
        userCoupon.reserve(orderId);

        userCoupon.releaseReservation();

        assertThat(userCoupon.getIsUsed()).isFalse();
        assertThat(userCoupon.getOrderId()).isNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 취소 성공 - isUsed가 false로 변경되고 orderId와 usedAt이 null이 된다")
    void cancelUse_Success() {
        UserCoupon userCoupon = createUserCoupon(true, LocalDateTime.now().plusDays(30));
        userCoupon = UserCoupon.builder()
                .id(userCoupon.getId())
                .couponId(userCoupon.getCouponId())
                .userId(userCoupon.getUserId())
                .orderId(100L)
                .isUsed(true)
                .issuedAt(userCoupon.getIssuedAt())
                .usedAt(LocalDateTime.now())
                .expiresAt(userCoupon.getExpiresAt())
                .updatedAt(userCoupon.getUpdatedAt())
                .build();

        userCoupon.cancelUse();

        assertThat(userCoupon.getIsUsed()).isFalse();
        assertThat(userCoupon.getOrderId()).isNull();
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 사용 취소 실패 - 사용되지 않은 쿠폰")
    void cancelUse_Fail_NotUsed() {
        UserCoupon userCoupon = createUserCoupon(false, LocalDateTime.now().plusDays(30));

        assertThatThrownBy(() -> userCoupon.cancelUse())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_USED);
    }

    private UserCoupon createUserCoupon(boolean isUsed, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return UserCoupon.builder()
                .id(1L)
                .couponId(100L)
                .userId(200L)
                .isUsed(isUsed)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .updatedAt(now)
                .build();
    }

    private UserCoupon createUserCouponWithOrder(boolean isUsed, Long orderId, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return UserCoupon.builder()
                .id(1L)
                .couponId(100L)
                .userId(200L)
                .orderId(orderId)
                .isUsed(isUsed)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .updatedAt(now)
                .build();
    }
}