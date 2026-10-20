package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.BedTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedTransferLogRepository extends JpaRepository<BedTransferLog, Long> {
    List<BedTransferLog> findByAdmissionIdOrderByTransferredAtAsc(Long admissionId);
}
