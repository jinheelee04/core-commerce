package com.hhplus.ecommerce.domain.coupon.service;

import com.hhplus.ecommerce.domain.coupon.dto.CouponResponse;
import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("CouponService 테스트")
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    private static final Long USER_ID = 1L;
    private static final Long COUPON_ID = 100L;
    private static final Long USER_COUPON_ID = 200L;

    @Test
    @DisplayName("발급 가능한 쿠폰 목록을 조회한다")
    void getAvailableCoupons() {
        Coupon coupon1 = createTestCoupon(COUPON_ID, "WELCOME", 100);
        Coupon coupon2 = createTestCoupon(COUPON_ID + 1, "SUMMER", 50);
        given(couponRepository.findIssuableCoupons()).willReturn(List.of(coupon1, coupon2));

        List<CouponResponse> result = couponService.getAvailableCoupons();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("WELCOME");
        assertThat(result.get(1).code()).isEqualTo("SUMMER");
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회한다")
    void getUserCoupons() {
        UserCoupon userCoupon1 = createTestUserCoupon(USER_COUPON_ID, false);
        UserCoupon userCoupon2 = createTestUserCoupon(USER_COUPON_ID + 1, true);
        given(userCouponRepository.findByUserId(USER_ID)).willReturn(List.of(userCoupon1, userCoupon2));
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(createTestCoupon(COUPON_ID, "WELCOME", 100)));

        List<UserCouponResponse> result = couponService.getUserCoupons(USER_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("사용자의 미사용 쿠폰만 조회한다")
    void getUnusedUserCoupons() {
        UserCoupon userCoupon = createTestUserCoupon(USER_COUPON_ID, false);
        given(userCouponRepository.findByUserIdAndIsUsed(USER_ID, false)).willReturn(List.of(userCoupon));
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(createTestCoupon(COUPON_ID, "WELCOME", 100)));

        List<UserCouponResponse> result = couponService.getUnusedUserCoupons(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isUsed()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 성공 - 정상적으로 쿠폰이 발급된다")
    void issueCoupon_Success() {
        Coupon coupon = createTestCoupon(COUPON_ID, "WELCOME", 100);
        UserCoupon issuedCoupon = createTestUserCoupon(USER_COUPON_ID, false);

        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
        given(userCouponRepository.findByCouponIdAndUserId(COUPON_ID, USER_ID)).willReturn(Optional.empty());
        given(userCouponRepository.generateNextId()).willReturn(USER_COUPON_ID);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(issuedCoupon);

        UserCouponResponse result = couponService.issueCoupon(USER_ID, COUPON_ID);

        assertThat(result).isNotNull();
        assertThat(result.userCouponId()).isEqualTo(USER_COUPON_ID);
        assertThat(result.couponId()).isEqualTo(COUPON_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.isUsed()).isFalse();
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 존재하지 않는 쿠폰")
    void issueCoupon_CouponNotFound() {
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급받은 쿠폰")
    void issueCoupon_AlreadyIssued() {
        Coupon coupon = createTestCoupon(COUPON_ID, "WELCOME", 100);
        UserCoupon existingUserCoupon = createTestUserCoupon(USER_COUPON_ID, false);

        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
        given(userCouponRepository.findByCouponIdAndUserId(COUPON_ID, USER_ID))
                .willReturn(Optional.of(existingUserCoupon)); // 이미 발급됨

        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 수량 소진")
    void issueCoupon_OutOfStock() {
        Coupon coupon = createTestCoupon(COUPON_ID, "WELCOME", 0);

        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
        // validateCouponIssuable()에서 remainingQuantity가 0이면 예외 발생

        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("쿠폰 예약 성공 - 정상적으로 쿠폰이 예약된다")
    void reserveCoupon_Success() {
        Long orderId = 300L;
        UserCoupon userCoupon = createTestUserCoupon(USER_COUPON_ID, false);

        given(userCouponRepository.findById(USER_COUPON_ID)).willReturn(Optional.of(userCoupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        couponService.reserveCoupon(USER_COUPON_ID, orderId);

        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 확정 성공 - 예약된 쿠폰이 확정된다")
    void confirmCouponReservation_Success() {
        Long orderId = 300L;
        UserCoupon userCoupon = createTestUserCouponWithOrder(USER_COUPON_ID, false, orderId);

        given(userCouponRepository.findById(USER_COUPON_ID)).willReturn(Optional.of(userCoupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        couponService.confirmCouponReservation(USER_COUPON_ID);

        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 예약 해제 성공 - 예약이 해제된다")
    void releaseCouponReservation_Success() {
        Long orderId = 300L;
        UserCoupon userCoupon = createTestUserCouponWithOrder(USER_COUPON_ID, false, orderId);

        given(userCouponRepository.findById(USER_COUPON_ID)).willReturn(Optional.of(userCoupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);

        couponService.releaseCouponReservation(USER_COUPON_ID);

        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 사용 취소 성공 - 쿠폰 사용이 취소된다")
    void cancelCouponUse_Success() {
        UserCoupon userCoupon = createTestUserCoupon(USER_COUPON_ID, true);
        Coupon coupon = createTestCoupon(COUPON_ID, "WELCOME", 50);

        given(userCouponRepository.findById(USER_COUPON_ID)).willReturn(Optional.of(userCoupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));
        given(couponRepository.save(any(Coupon.class))).willReturn(coupon);

        couponService.cancelCouponUse(USER_COUPON_ID);

        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 조회 성공 - ID로 쿠폰을 조회한다")
    void getCoupon_Success() {
        Coupon coupon = createTestCoupon(COUPON_ID, "WELCOME", 100);
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.of(coupon));

        CouponResponse result = couponService.getCoupon(COUPON_ID);

        assertThat(result).isNotNull();
        assertThat(result.couponId()).isEqualTo(COUPON_ID);
        assertThat(result.code()).isEqualTo("WELCOME");
    }

    @Test
    @DisplayName("쿠폰 조회 실패 - 존재하지 않는 쿠폰")
    void getCoupon_NotFound() {
        given(couponRepository.findById(COUPON_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCoupon(COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    @DisplayName("쿠폰 코드로 조회 성공")
    void getCouponByCode_Success() {
        String code = "WELCOME";
        Coupon coupon = createTestCoupon(COUPON_ID, code, 100);
        given(couponRepository.findByCode(code)).willReturn(Optional.of(coupon));

        CouponResponse result = couponService.getCouponByCode(code);

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo(code);
    }

    @Test
    @DisplayName("쿠폰 코드로 조회 실패 - 존재하지 않는 코드")
    void getCouponByCode_NotFound() {
        String code = "INVALID";
        given(couponRepository.findByCode(code)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponByCode(code))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_FOUND);
    }

    private Coupon createTestCoupon(Long id, String code, int remainingQuantity) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder()
                .id(id)
                .code(code)
                .name("테스트 쿠폰")
                .description("테스트용 쿠폰입니다")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(remainingQuantity)
                .startsAt(now.minusDays(1))
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserCoupon createTestUserCoupon(Long id, boolean isUsed) {
        LocalDateTime now = LocalDateTime.now();
        return UserCoupon.builder()
                .id(id)
                .couponId(COUPON_ID)
                .userId(USER_ID)
                .isUsed(isUsed)
                .issuedAt(now)
                .expiresAt(now.plusDays(30))
                .updatedAt(now)
                .build();
    }

    private UserCoupon createTestUserCouponWithOrder(Long id, boolean isUsed, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        return UserCoupon.builder()
                .id(id)
                .couponId(COUPON_ID)
                .userId(USER_ID)
                .orderId(orderId)
                .isUsed(isUsed)
                .issuedAt(now)
                .expiresAt(now.plusDays(30))
                .updatedAt(now)
                .build();
    }
}