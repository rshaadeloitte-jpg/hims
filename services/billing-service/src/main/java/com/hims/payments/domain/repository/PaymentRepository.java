package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillIdOrderByPaymentIdAsc(Long billId);
}
