package com.hhplus.ecommerce.domain.coupon.service;

import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.DiscountType;
import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("CouponService 단위 테스트")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private com.hhplus.ecommerce.domain.user.repository.UserRepository userRepository;

    @InjectMocks
    private CouponService couponService;

    private static final Long USER_ID = 1L;
    private static final Long COUPON_ID = 100L;

    private Coupon testCoupon;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(USER_ID);

        testCoupon = new Coupon(
                "WELCOME10",
                "신규 회원 할인",
                "10% 할인 쿠폰",
                DiscountType.PERCENTAGE,
                10,
                10000L,
                5000L,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        setId(testCoupon, COUPON_ID);
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setIssuedAt(UserCoupon userCoupon, LocalDateTime issuedAt) {
        try {
            java.lang.reflect.Field field = UserCoupon.class.getDeclaredField("issuedAt");
            field.setAccessible(true);
            field.set(userCoupon, issuedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("쿠폰 발급 성공 - 정상적으로 쿠폰이 발급된다")
    void issueCoupon_Success() {
        // Given
        UserCoupon issuedCoupon = new UserCoupon(testCoupon, mockUser, LocalDateTime.now().plusDays(30));
        setId(issuedCoupon, 200L);
        setIssuedAt(issuedCoupon, LocalDateTime.now());

        given(couponRepository.findByIdWithLock(COUPON_ID)).willReturn(Optional.of(testCoupon));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(userCouponRepository.existsByCouponIdAndUserId(COUPON_ID, USER_ID)).willReturn(false);
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(issuedCoupon);

        // When
        UserCouponResponse result = couponService.issueCoupon(USER_ID, COUPON_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userCouponId()).isEqualTo(200L);
        assertThat(result.isUsed()).isFalse();
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 존재하지 않는 쿠폰")
    void issueCoupon_CouponNotFound() {
        // Given
        given(couponRepository.findByIdWithLock(COUPON_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 이미 발급받은 쿠폰")
    void issueCoupon_AlreadyIssued() {
        // Given
        given(couponRepository.findByIdWithLock(COUPON_ID)).willReturn(Optional.of(testCoupon));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(userCouponRepository.existsByCouponIdAndUserId(COUPON_ID, USER_ID)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 쿠폰 수량 소진")
    void issueCoupon_OutOfStock() {
        // Given
        Coupon exhaustedCoupon = new Coupon(
                "SOLDOUT",
                "품절 쿠폰",
                "품절",
                DiscountType.PERCENTAGE,
                10,
                10000L,
                null,
                0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        setId(exhaustedCoupon, COUPON_ID);

        given(couponRepository.findByIdWithLock(COUPON_ID)).willReturn(Optional.of(exhaustedCoupon));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(userCouponRepository.existsByCouponIdAndUserId(COUPON_ID, USER_ID)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> couponService.issueCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_OUT_OF_STOCK);
    }
}
