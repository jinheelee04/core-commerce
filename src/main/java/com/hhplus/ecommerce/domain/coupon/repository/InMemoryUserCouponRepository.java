package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.model.UserCoupon;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        InMemoryDataStore.USER_COUPONS.put(userCoupon.getId(), userCoupon);
        return userCoupon;
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.USER_COUPONS.get(id));
    }

    @Override
    public Optional<UserCoupon> findByCouponIdAndUserId(Long couponId, Long userId) {
        return InMemoryDataStore.USER_COUPONS.values().stream()
                .filter(uc -> uc.getCouponId().equals(couponId) && uc.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return InMemoryDataStore.USER_COUPONS.values().stream()
                .filter(uc -> uc.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<UserCoupon> findByUserIdAndIsUsed(Long userId, Boolean isUsed) {
        return InMemoryDataStore.USER_COUPONS.values().stream()
                .filter(uc -> uc.getUserId().equals(userId) && uc.getIsUsed().equals(isUsed))
                .toList();
    }

    @Override
    public Optional<UserCoupon> findByOrderId(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return InMemoryDataStore.USER_COUPONS.values().stream()
                .filter(uc -> orderId.equals(uc.getOrderId()))
                .findFirst();
    }

    @Override
    public List<UserCoupon> findAll() {
        return List.copyOf(InMemoryDataStore.USER_COUPONS.values());
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.USER_COUPONS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.userCouponIdSequence.incrementAndGet();
    }
}
