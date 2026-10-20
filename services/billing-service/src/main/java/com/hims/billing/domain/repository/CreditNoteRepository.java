package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    List<CreditNote> findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(Long billId);
}
