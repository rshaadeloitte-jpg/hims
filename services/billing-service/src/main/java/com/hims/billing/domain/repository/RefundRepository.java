package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByBillIdOrderByRefundIdAsc(Long billId);
}
