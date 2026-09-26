package com.dawn.payment.repository;

import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(String reservationId);

    List<Payment> findAllByStatusOrderByCreatedAtDesc(PaymentStatus status);

    Optional<Payment> findByGatewayTxnRef(String gatewayTxnRef);

    Optional<Payment> findFirstByReservationIdOrderByCreatedAtDesc(String reservationId);
}
