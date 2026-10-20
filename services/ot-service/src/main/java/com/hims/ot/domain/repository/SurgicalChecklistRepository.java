package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.SurgicalChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurgicalChecklistRepository extends JpaRepository<SurgicalChecklist, Long> {
    List<SurgicalChecklist> findByScheduleId(Long scheduleId);
    List<SurgicalChecklist> findByPatientIdAndPhase(Long patientId, String phase);
}
