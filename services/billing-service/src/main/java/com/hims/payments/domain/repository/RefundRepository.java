package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByBillIdOrderByRefundIdAsc(Long billId);
}
