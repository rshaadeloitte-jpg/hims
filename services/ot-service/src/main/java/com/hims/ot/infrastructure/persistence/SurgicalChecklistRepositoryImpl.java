package com.hims.ot.infrastructure.persistence;

import com.hims.ot.domain.model.SurgicalChecklist;
import com.hims.ot.domain.repository.SurgicalChecklistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SurgicalChecklistRepositoryImpl {

    private final SurgicalChecklistRepository jpaRepository;

    public SurgicalChecklistRepositoryImpl(SurgicalChecklistRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<SurgicalChecklist> findByScheduleId(Long scheduleId) {
        return jpaRepository.findByScheduleId(scheduleId);
    }

    public List<SurgicalChecklist> findByPatientIdAndPhase(Long patientId, String phase) {
        return jpaRepository.findByPatientIdAndPhase(patientId, phase);
    }

    public List<SurgicalChecklist> findAll() {
        return jpaRepository.findAll();
    }

    public SurgicalChecklist save(SurgicalChecklist entity) {
        return jpaRepository.save(entity);
    }
}
