package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.PaymentModeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentModeDetailRepository extends JpaRepository<PaymentModeDetail, Long> {

    List<PaymentModeDetail> findByPaymentIdOrderByModeDetailIdAsc(Long paymentId);
}
