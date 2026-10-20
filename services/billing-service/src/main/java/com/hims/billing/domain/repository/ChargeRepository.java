package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {

    List<Charge> findByBillIdOrderByChargeIdAsc(Long billId);
}
