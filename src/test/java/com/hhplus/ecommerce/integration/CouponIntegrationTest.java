package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.DiscountType;
import com.hhplus.ecommerce.domain.coupon.entity.UserCoupon;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.order.entity.Order;
import com.hhplus.ecommerce.domain.order.repository.OrderRepository;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.domain.user.entity.UserAddress;
import com.hhplus.ecommerce.domain.user.repository.UserAddressRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import com.hhplus.ecommerce.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 쿠폰 발급/사용 통합 테스트
 * - 쿠폰 발급 → 쿠폰 사용 → 쿠폰 복구
 * - 동시성 테스트: 선착순 쿠폰 발급
 */
@DisplayName("통합 테스트: 쿠폰 플로우")
class CouponIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setUpTestData() {
        user = new User("test@example.com", "테스트유저", "01012345678");
        em.persist(user);

        coupon = new Coupon(
                "WELCOME2024",
                "신규가입 환영 쿠폰",
                "첫 구매 시 10% 할인",
                DiscountType.PERCENTAGE,
                10,
                50000L,
                10000L,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        em.persist(coupon);

        flushAndClear();
    }

    @Test
    @DisplayName("쿠폰 발급 시 재고가 차감되고 사용자 쿠폰이 생성되어야 한다")
    void issueCoupon_Success() {
        // When: 쿠폰 발급
        UserCouponResponse result = couponService.issueCoupon(user.getId(), coupon.getId());
        flushAndClear();

        // Then: 쿠폰 발급 성공
        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("WELCOME2024");
        assertThat(result.isUsed()).isFalse();

        // 쿠폰 재고 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰은 중복 발급할 수 없어야 한다")
    void issueCoupon_Duplicate_Fail() {
        // Given: 쿠폰 발급
        couponService.issueCoupon(user.getId(), coupon.getId());
        flushAndClear();

        // When & Then: 중복 발급 시도 실패
        assertThatThrownBy(() ->
                couponService.issueCoupon(user.getId(), coupon.getId())
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 재고가 소진되면 발급이 실패해야 한다")
    void issueCoupon_OutOfStock_Fail() {
        // Given: 재고 1개인 쿠폰
        Coupon limitedCoupon = new Coupon(
                "LIMITED",
                "한정 쿠폰",
                "선착순 1명",
                DiscountType.FIXED_AMOUNT,
                5000,
                0L,
                null,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        em.persist(limitedCoupon);

        User user2 = new User("user2@example.com", "유저2", "01012341234");
        em.persist(user2);

        flushAndClear();

        // When: 첫 번째 발급 성공
        couponService.issueCoupon(user.getId(), limitedCoupon.getId());

        // Then: 두 번째 발급 실패
        assertThatThrownBy(() ->
                couponService.issueCoupon(user2.getId(), limitedCoupon.getId())
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용한 쿠폰을 취소하면 다시 사용 가능 상태가 되고 쿠폰 재고가 복구되어야 한다")
    void cancelCoupon_RestoreStockAndUsability() {
        // Given: 쿠폰 발급 및 사용
        UserCouponResponse issuedCoupon = couponService.issueCoupon(user.getId(), coupon.getId());
        flushAndClear();

        UserAddress address = userAddressRepository.save(
                new UserAddress(user, "집", "홍길동", "010-1234-5678", "12345", "서울시 강남구 역삼동", "101호", true)
        );

        Order order = orderRepository.save(new Order(
                user,
                address,
                issuedCoupon.userCouponId(),
                "ORD-" + UUID.randomUUID(),
                10000L,
                1000L,
                9000L,
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구 역삼동",
                "101호",
                "문 앞에 두세요"
        ));
        flushAndClear();

        // 쿠폰 사용 처리 (reserveCoupon으로 사용 상태로 변경)
        couponService.reserveCoupon(issuedCoupon.userCouponId(), order.getId());
        flushAndClear();

        // 쿠폰 재고 확인 (사용 후 99개로 감소)
        Coupon usedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(usedCoupon.getRemainingQuantity()).isEqualTo(99);

        // When: 쿠폰 사용 취소
        couponService.cancelCouponUse(issuedCoupon.userCouponId());
        flushAndClear();

        // Then: 쿠폰 재고 복구
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(100);

        // 사용자 쿠폰 상태 확인
        UserCoupon cancelledUserCoupon = userCouponRepository.findById(issuedCoupon.userCouponId()).orElseThrow();
        assertThat(cancelledUserCoupon.getIsUsed()).isFalse();
    }
}
