package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.BillPaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillPaymentLedgerRepository extends JpaRepository<BillPaymentLedger, Long> {
    List<BillPaymentLedger> findByBillIdOrderByAddedDatetimeDesc(Long billId);
    List<BillPaymentLedger> findByBillIdAndEventType(Long billId, String eventType);
}
