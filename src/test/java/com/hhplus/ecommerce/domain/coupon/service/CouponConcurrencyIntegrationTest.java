package com.hhplus.ecommerce.domain.coupon.service;

import com.hhplus.ecommerce.domain.coupon.dto.UserCouponResponse;
import com.hhplus.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.model.DiscountType;
import com.hhplus.ecommerce.domain.coupon.repository.InMemoryCouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.InMemoryUserCouponRepository;
import com.hhplus.ecommerce.global.exception.BusinessException;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("쿠폰 동시성 통합 테스트")
class CouponConcurrencyIntegrationTest {

    private CouponService couponService;
    private InMemoryCouponRepository couponRepository;
    private InMemoryUserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        InMemoryDataStore.clear();
        couponRepository = new InMemoryCouponRepository();
        userCouponRepository = new InMemoryUserCouponRepository();
        couponService = new CouponService(couponRepository, userCouponRepository);
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 - 100명이 50개 쿠폰 발급 시도 시 정확히 50명만 성공")
    void issueCoupon_Concurrency_FirstComeFirstServed() throws InterruptedException {
        // Given
        int totalQuantity = 50;
        int userCount = 100;
        Long couponId = createTestCoupon("FIRSTCOME", totalQuantity);

        CountDownLatch latch = new CountDownLatch(userCount);
        ExecutorService executor = Executors.newFixedThreadPool(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(finalUserId, couponId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == CouponErrorCode.COUPON_OUT_OF_STOCK) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(totalQuantity);
        assertThat(failCount.get()).isEqualTo(userCount - totalQuantity);

        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getRemainingQuantity()).isZero();
    }

    @Test
    @DisplayName("중복 발급 방지 - 같은 사용자가 동시에 여러 번 시도해도 1번만 발급")
    void issueCoupon_Concurrency_PreventDuplicate() throws InterruptedException {
        // Given
        Long couponId = createTestCoupon("NODEDUP", 100);
        Long userId = 1L;
        int attemptCount = 10;

        CountDownLatch latch = new CountDownLatch(attemptCount);
        ExecutorService executor = Executors.newFixedThreadPool(attemptCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < attemptCount; i++) {
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == CouponErrorCode.COUPON_ALREADY_ISSUED) {
                        duplicateCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(attemptCount - 1);

        List<UserCouponResponse> userCoupons = couponService.getUserCoupons(userId);
        assertThat(userCoupons).hasSize(1);

        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        assertThat(coupon.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("쿠폰 발급/취소 동시성 - 발급과 취소가 동시에 발생해도 수량 정합성 유지")
    void issueCoupon_Concurrency_IssueAndCancel() throws InterruptedException {
        // Given
        int initialQuantity = 20;
        Long couponId = createTestCoupon("ISSUECANCEL", initialQuantity);

        List<Long> issuedUserCouponIds = new ArrayList<>();
        for (long userId = 1; userId <= 10; userId++) {
            UserCouponResponse response = couponService.issueCoupon(userId, couponId);
            issuedUserCouponIds.add(response.userCouponId());
        }

        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger newIssueSuccess = new AtomicInteger(0);
        AtomicInteger cancelSuccess = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            long userId = 11L + i;
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(userId, couponId);
                    newIssueSuccess.incrementAndGet();
                } catch (BusinessException e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < 10; i++) {
            Long userCouponId = issuedUserCouponIds.get(i);
            executor.submit(() -> {
                try {
                    couponService.useCoupon(userCouponId, 999L);
                    couponService.cancelCouponUse(userCouponId);
                    cancelSuccess.incrementAndGet();
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        Coupon finalCoupon = couponRepository.findById(couponId).orElseThrow();
        int expectedRemaining = initialQuantity - 10 - newIssueSuccess.get() + cancelSuccess.get();
        assertThat(finalCoupon.getRemainingQuantity()).isEqualTo(expectedRemaining);
    }

    private Long createTestCoupon(String code, int quantity) {
        LocalDateTime now = LocalDateTime.now();
        Long couponId = couponRepository.generateNextId();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .code(code)
                .name("동시성 테스트 쿠폰")
                .description("테스트용 쿠폰입니다")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .minOrderAmount(10000L)
                .maxDiscountAmount(5000L)
                .totalQuantity(quantity)
                .remainingQuantity(quantity)
                .startsAt(now.minusDays(1))
                .endsAt(now.plusDays(30))
                .status(CouponStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return couponRepository.save(coupon).getId();
    }
}
