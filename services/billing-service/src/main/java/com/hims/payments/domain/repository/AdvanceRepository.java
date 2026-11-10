package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.Advance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvanceRepository extends JpaRepository<Advance, Long> {

    /** BR-BILL-007 FIFO order = advance_id ascending. */
    List<Advance> findByPatientIdOrderByAdvanceIdAsc(Long patientId);
}
