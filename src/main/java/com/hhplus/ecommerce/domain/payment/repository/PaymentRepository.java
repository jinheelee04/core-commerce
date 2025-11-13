package com.hhplus.ecommerce.domain.payment.repository;

import com.hhplus.ecommerce.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    List<Payment> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.order " +
           "WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") Payment.PaymentStatus status);

    Optional<Payment> findByClientRequestId(String clientRequestId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(Payment.PaymentStatus status);
}
