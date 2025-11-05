package com.hhplus.ecommerce.domain.payment.repository;

import com.hhplus.ecommerce.domain.payment.model.Payment;
import com.hhplus.ecommerce.domain.payment.model.PaymentStatus;
import com.hhplus.ecommerce.global.storage.InMemoryDataStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    @Override
    public Payment save(Payment payment) {
        InMemoryDataStore.PAYMENTS.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(InMemoryDataStore.PAYMENTS.get(id));
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return InMemoryDataStore.PAYMENTS.values().stream()
                .filter(payment -> payment.getOrderId().equals(orderId))
                .findFirst();
    }

    @Override
    public Optional<Payment> findByClientRequestId(String clientRequestId) {
        if (clientRequestId == null) {
            return Optional.empty();
        }
        return InMemoryDataStore.PAYMENTS.values().stream()
                .filter(payment -> clientRequestId.equals(payment.getClientRequestId()))
                .findFirst();
    }

    @Override
    public Optional<Payment> findByTransactionId(String transactionId) {
        if (transactionId == null) {
            return Optional.empty();
        }
        return InMemoryDataStore.PAYMENTS.values().stream()
                .filter(payment -> transactionId.equals(payment.getTransactionId()))
                .findFirst();
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return InMemoryDataStore.PAYMENTS.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .toList();
    }

    @Override
    public List<Payment> findAll() {
        return List.copyOf(InMemoryDataStore.PAYMENTS.values());
    }

    @Override
    public void deleteById(Long id) {
        InMemoryDataStore.PAYMENTS.remove(id);
    }

    @Override
    public Long generateNextId() {
        return InMemoryDataStore.paymentIdSequence.incrementAndGet();
    }
}
