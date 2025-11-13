package com.hhplus.ecommerce.domain.coupon.model;

import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.entity.DiscountType;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Coupon 도메인 모델 테스트")
class CouponTest {

    @Test
    @DisplayName("쿠폰 생성 시 기본 상태는 ACTIVE이고 remainingQuantity는 totalQuantity와 동일하다")
    void create() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = now.minusDays(1);
        LocalDateTime endsAt = now.plusDays(30);

        Coupon coupon = Coupon.create(
                1L, "WELCOME", "신규회원 쿠폰", "첫 구매 시 10% 할인",
                DiscountType.PERCENTAGE, 10, 10000L, 5000L,
                100, startsAt, endsAt
        );

        assertThat(coupon.getId()).isEqualTo(1L);
        assertThat(coupon.getCode()).isEqualTo("WELCOME");
        assertThat(coupon.getTotalQuantity()).isEqualTo(100);
        assertThat(coupon.getRemainingQuantity()).isEqualTo(100);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(coupon.getCreatedAt()).isNotNull();
        assertThat(coupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 - ACTIVE 상태이고 수량이 남아있으며 종료일 이전인 경우")
    void isIssuable_True() {
        Coupon coupon = createActiveCoupon(100);

        assertThat(coupon.isIssuable()).isTrue();
    }

    @Test
    @DisplayName("발급 불가능한 쿠폰 - 수량이 소진된 경우")
    void isIssuable_False_NoQuantity() {
        Coupon coupon = createActiveCoupon(0);

        assertThat(coupon.isIssuable()).isFalse();
    }

    @Test
    @DisplayName("발급 불가능한 쿠폰 - 만료된 경우")
    void isIssuable_False_Expired() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("EXPIRED")
                .name("만료 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(50)
                .startsAt(now.minusDays(30))
                .endsAt(now.minusDays(1))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(coupon.isIssuable()).isFalse();
    }

    @Test
    @DisplayName("사용 가능한 쿠폰 - ACTIVE 상태이고 사용 기간 내인 경우")
    void isUsable_True() {
        Coupon coupon = createActiveCoupon(100);

        assertThat(coupon.isUsable()).isTrue();
    }

    @Test
    @DisplayName("사용 불가능한 쿠폰 - 시작일 이전인 경우")
    void isUsable_False_NotStarted() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("FUTURE")
                .name("미래 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(100)
                .startsAt(now.plusDays(1))
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(coupon.isUsable()).isFalse();
    }

    @Test
    @DisplayName("쿠폰 발급 성공 - remainingQuantity가 1 감소한다")
    void issue_Success() {
        Coupon coupon = createActiveCoupon(100);
        int beforeQuantity = coupon.getRemainingQuantity();

        coupon.issue();

        assertThat(coupon.getRemainingQuantity()).isEqualTo(beforeQuantity - 1);
        assertThat(coupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 발급 불가능한 상태")
    void issue_Fail_NotIssuable() {
        Coupon coupon = createActiveCoupon(0);

        assertThatThrownBy(() -> coupon.issue())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_OUT_OF_STOCK);
    }

    @Test
    @DisplayName("쿠폰 발급 취소 성공 - remainingQuantity가 1 증가한다")
    void cancelIssue_Success() {
        Coupon coupon = createActiveCoupon(50);
        int beforeQuantity = coupon.getRemainingQuantity();

        coupon.cancelIssue();

        assertThat(coupon.getRemainingQuantity()).isEqualTo(beforeQuantity + 1);
        assertThat(coupon.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("쿠폰 발급 취소 실패 - remainingQuantity가 totalQuantity 이상인 경우")
    void cancelIssue_Fail() {
        Coupon coupon = createActiveCoupon(100);

        assertThatThrownBy(() -> coupon.cancelIssue())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_CANNOT_CANCEL_ISSUE);
    }

    @Test
    @DisplayName("퍼센트 할인 계산 - 10% 할인")
    void calculateDiscount_Percentage() {
        Coupon coupon = createActiveCoupon(100);
        long orderAmount = 50000L;

        long discount = coupon.calculateDiscount(orderAmount);

        assertThat(discount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("퍼센트 할인 계산 - 최대 할인 금액 제한 적용")
    void calculateDiscount_Percentage_MaxDiscount() {
        Coupon coupon = createActiveCoupon(100);
        long orderAmount = 100000L;

        long discount = coupon.calculateDiscount(orderAmount);

        assertThat(discount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("고정 금액 할인 계산")
    void calculateDiscount_FixedAmount() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("FIXED")
                .name("고정할인 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(3000)
                .minOrderAmount(10000L)
                .maxDiscountAmount(3000L)
                .totalQuantity(100)
                .remainingQuantity(100)
                .startsAt(now.minusDays(1))
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        long discount = coupon.calculateDiscount(50000L);

        assertThat(discount).isEqualTo(3000L);
    }

    @Test
    @DisplayName("할인 계산 실패 - 최소 주문 금액 미충족")
    void calculateDiscount_Fail_MinOrderAmount() {
        Coupon coupon = createActiveCoupon(100);
        long orderAmount = 5000L;

        assertThatThrownBy(() -> coupon.calculateDiscount(orderAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }

    @Test
    @DisplayName("할인 계산 실패 - 사용 불가능한 쿠폰")
    void calculateDiscount_Fail_NotUsable() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("EXPIRED")
                .name("만료 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(100)
                .remainingQuantity(100)
                .startsAt(now.minusDays(30))
                .endsAt(now.minusDays(1))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThatThrownBy(() -> coupon.calculateDiscount(50000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CouponErrorCode.COUPON_NOT_USABLE);
    }

    private Coupon createActiveCoupon(int remainingQuantity) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder()
                .id(1L)
                .code("WELCOME")
                .name("신규회원 쿠폰")
                .description("첫 구매 시 10% 할인")
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
}