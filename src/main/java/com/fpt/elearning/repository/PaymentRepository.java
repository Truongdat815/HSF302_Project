package com.fpt.elearning.repository;

import com.fpt.elearning.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransferCode(String transferCode);
    Optional<Payment> findByOrder_Id(Long orderId);
}
