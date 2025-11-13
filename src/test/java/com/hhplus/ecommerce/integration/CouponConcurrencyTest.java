package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.domain.coupon.entity.Coupon;
import com.hhplus.ecommerce.domain.coupon.entity.DiscountType;
import com.hhplus.ecommerce.domain.coupon.repository.CouponRepository;
import com.hhplus.ecommerce.domain.coupon.repository.UserCouponRepository;
import com.hhplus.ecommerce.domain.coupon.service.CouponService;
import com.hhplus.ecommerce.domain.user.entity.User;
import com.hhplus.ecommerce.domain.user.repository.UserRepository;
import com.hhplus.ecommerce.support.IntegrationTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 발급 동시성 테스트
 * - 선착순 쿠폰 발급 시 동시성 제어 검증
 * - Pessimistic Lock 기반 동시성 제어
 */
@Slf4j
@DisplayName("통합 테스트: 쿠폰 발급 동시성")
class CouponConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserRepository userRepository;

    private Coupon limitedCoupon;
    private List<User> users;

    @BeforeEach
    void setUpTestData() {
        // 선착순 10명 한정 쿠폰 생성
        limitedCoupon = new Coupon(
                "FIRST10",
                "선착순 10명 할인 쿠폰",
                "빠른 사람만! 10% 할인",
                DiscountType.PERCENTAGE,
                10,
                50000L,
                10000L,
                10, // 총 10개만 발급 가능
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        em.persist(limitedCoupon);

        // 20명의 사용자 생성 (10명만 쿠폰을 받을 수 있음)
        users = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User user = new User(
                    "user" + i + "@example.com",
                    "테스트유저" + i,
                    "010-0000-" + String.format("%04d", i)
            );
            em.persist(user);
            users.add(user);
        }

        flushAndClear();
    }

    @Test
    @DisplayName("20명이 동시에 선착순 10개 쿠폰을 요청하면 정확히 10명만 발급받아야 한다")
    void issueCoupon_Concurrency_OnlyTenSucceed() throws InterruptedException {
        // Given: 20명의 사용자, 10개의 쿠폰
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 쿠폰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    User user = userRepository.findById(users.get(index).getId()).orElseThrow();
                    couponService.issueCoupon(user.getId(), limitedCoupon.getId());
                    successCount.incrementAndGet();
                    log.info("[성공] 사용자 {} 쿠폰 발급 성공", user.getEmail());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.info("[실패] 사용자 {} 쿠폰 발급 실패: {}", users.get(index).getEmail(), e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 10명만 성공, 10명은 실패
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(10);

        // DB에서 실제 발급된 쿠폰 개수 확인
        long issuedCount = userCouponRepository.count();
        assertThat(issuedCount).isEqualTo(10);

        // 쿠폰 잔여 수량 확인
        Coupon updatedCoupon = couponRepository.findById(limitedCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);

        log.info("=== 동시성 테스트 결과 ===");
        log.info("성공: {}명, 실패: {}명", successCount.get(), failCount.get());
        log.info("DB 발급 쿠폰 수: {}개", issuedCount);
        log.info("쿠폰 잔여 수량: {}개", updatedCoupon.getRemainingQuantity());
    }

    @Test
    @DisplayName("100명이 동시에 선착순 50개 쿠폰을 요청하면 정확히 50명만 발급받아야 한다")
    void issueCoupon_HighConcurrency_OnlyFiftySucceed() throws InterruptedException {
        // Given: 100명의 사용자, 50개의 쿠폰
        Coupon highDemandCoupon = new Coupon(
                "FLASH50",
                "플래시 세일 쿠폰",
                "선착순 50명! 20% 할인",
                DiscountType.PERCENTAGE,
                20,
                100000L,
                20000L,
                50, // 총 50개만 발급 가능
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
        em.persist(highDemandCoupon);

        List<User> manyUsers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            User user = new User(
                    "concurrent" + i + "@example.com",
                    "동시사용자" + i,
                    "010-1111-" + String.format("%04d", i)
            );
            em.persist(user);
            manyUsers.add(user);
        }

        flushAndClear();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명이 동시에 쿠폰 발급 요청
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    User user = userRepository.findById(manyUsers.get(index).getId()).orElseThrow();
                    couponService.issueCoupon(user.getId(), highDemandCoupon.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 정확히 50명만 성공, 50명은 실패
        assertThat(successCount.get()).isEqualTo(50);
        assertThat(failCount.get()).isEqualTo(50);

        // DB에서 실제 발급된 쿠폰 개수 확인
        long issuedCount = userCouponRepository.countByCouponId(highDemandCoupon.getId());
        assertThat(issuedCount).isEqualTo(50);

        // 쿠폰 잔여 수량 확인
        Coupon updatedCoupon = couponRepository.findById(highDemandCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(0);

        log.info("=== 고부하 동시성 테스트 결과 ===");
        log.info("성공: {}명, 실패: {}명", successCount.get(), failCount.get());
        log.info("DB 발급 쿠폰 수: {}개", issuedCount);
        log.info("쿠폰 잔여 수량: {}개", updatedCoupon.getRemainingQuantity());
        log.info("소요 시간: {}ms", duration);
    }

    @Test
    @DisplayName("동일 사용자가 동시에 여러 번 요청해도 1번만 발급되어야 한다")
    void issueCoupon_SameUser_OnlyOnce() throws InterruptedException {
        // Given: 1명의 사용자가 10번 동시 요청
        User singleUser = users.get(0);
        int requestCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동일 사용자가 10번 동시 요청
        for (int i = 0; i < requestCount; i++) {
            executorService.execute(() -> {
                try {
                    User user = userRepository.findById(singleUser.getId()).orElseThrow();
                    couponService.issueCoupon(user.getId(), limitedCoupon.getId());
                    successCount.incrementAndGet();
                    log.info("[성공] 사용자 {} 쿠폰 발급 성공", user.getEmail());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.info("[실패] 사용자 {} 쿠폰 발급 실패: {}", singleUser.getEmail(), e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1번만 성공, 9번은 실패 (중복 발급 방지)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        // 해당 사용자가 발급받은 쿠폰 개수 확인
        long userCouponCount = userCouponRepository.countByUserIdAndCouponId(
                singleUser.getId(),
                limitedCoupon.getId()
        );
        assertThat(userCouponCount).isEqualTo(1);

        log.info("=== 중복 발급 방지 테스트 결과 ===");
        log.info("성공: {}번, 실패: {}번", successCount.get(), failCount.get());
        log.info("사용자 {} 발급 쿠폰 수: {}개", singleUser.getEmail(), userCouponCount);
    }
}
