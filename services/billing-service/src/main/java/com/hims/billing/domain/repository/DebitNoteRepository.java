package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    List<DebitNote> findByBillId(Long billId);
    List<DebitNote> findByPatientIdOrderByAddedDatetimeDesc(Long patientId);
}
