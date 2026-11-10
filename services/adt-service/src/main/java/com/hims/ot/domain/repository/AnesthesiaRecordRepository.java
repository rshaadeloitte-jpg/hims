package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.AnesthesiaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnesthesiaRecordRepository extends JpaRepository<AnesthesiaRecord, Long> {
    List<AnesthesiaRecord> findByScheduleId(Long scheduleId);
    List<AnesthesiaRecord> findByPatientId(Long patientId);
}
