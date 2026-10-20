package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.AdvanceConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvanceConsumptionRepository extends JpaRepository<AdvanceConsumption, Long> {

    List<AdvanceConsumption> findByPaymentIdOrderByConsumptionIdAsc(Long paymentId);
}
