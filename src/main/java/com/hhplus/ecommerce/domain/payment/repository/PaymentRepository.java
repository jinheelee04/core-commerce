package com.hhplus.ecommerce.domain.payment.repository;

import com.hhplus.ecommerce.domain.payment.model.Payment;
import com.hhplus.ecommerce.domain.payment.model.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByClientRequestId(String clientRequestId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findAll();
    void deleteById(Long id);
    Long generateNextId();
}
