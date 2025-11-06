package com.hhplus.ecommerce.domain.coupon.repository;

import com.hhplus.ecommerce.domain.coupon.model.Coupon;
import com.hhplus.ecommerce.domain.coupon.model.CouponStatus;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public class InMemoryCouponRepository implements CouponRepository {

    @Override
    public Coupon save(Coupon coupon) {
        InMemoryDataStore.COUPONS.put(coupon.getId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.COUPONS.get(id));
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return InMemoryDataStore.COUPONS.values().stream()
                .filter(coupon -> coupon.getCode().equals(code))
                .findFirst();
    }

    @Override
    public List<Coupon> findByStatus(CouponStatus status) {
        return InMemoryDataStore.COUPONS.values().stream()
                .filter(coupon -> coupon.getStatus() == status)
                .toList();
    }

    @Override
    public List<Coupon> findIssuableCoupons() {
        return InMemoryDataStore.COUPONS.values().stream()
                .filter(Coupon::isIssuable)
                .toList();
    }

    @Override
    public List<Coupon> findAll() {
        return List.copyOf(InMemoryDataStore.COUPONS.values());
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.COUPONS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.couponIdSequence.incrementAndGet();
    }
}
