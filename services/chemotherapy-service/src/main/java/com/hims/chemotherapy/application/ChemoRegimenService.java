package com.hims.chemotherapy.application;

import com.hims.chemotherapy.domain.model.ChemoRegimen;
import com.hims.chemotherapy.repository.ChemoRegimenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class ChemoRegimenService {

    private final ChemoRegimenRepository repository;

    public ChemoRegimenService(ChemoRegimenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChemoRegimen> getAllRegimens(Long patientId, String status) {
        if (patientId != null && status != null && !status.isBlank()) {
            return repository.findByPatientIdAndStatus(patientId, status);
        } else if (patientId != null) {
            return repository.findByPatientId(patientId);
        } else if (status != null && !status.isBlank()) {
            return repository.findByStatus(status);
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ChemoRegimen> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public ChemoRegimen getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ChemoRegimen not found with id: " + id));
    }

    public ChemoRegimen createRegimen(ChemoRegimen regimen) {
        if (regimen.getPatientId() == null) {
            throw new IllegalArgumentException("Patient ID must not be null");
        }
        if (regimen.getRegimenName() == null || regimen.getRegimenName().isBlank()) {
            throw new IllegalArgumentException("Regimen name must not be blank");
        }
        if (regimen.getTotalCycles() == null || regimen.getTotalCycles() < 1) {
            regimen.setTotalCycles(1);
        }
        if (regimen.getCurrentCycle() == null || regimen.getCurrentCycle() < 1) {
            regimen.setCurrentCycle(1);
        }
        if (regimen.getStatus() == null || regimen.getStatus().isBlank()) {
            regimen.setStatus(ChemoRegimen.STATUS_PLANNED);
        }
        return repository.save(regimen);
    }

    public ChemoRegimen updateRegimen(Long id, ChemoRegimen updated) {
        ChemoRegimen existing = getById(id);
        if (updated.getPatientName() != null) {
            existing.setPatientName(updated.getPatientName());
        }
        if (updated.getRegimenName() != null) {
            existing.setRegimenName(updated.getRegimenName());
        }
        if (updated.getProtocolCode() != null) {
            existing.setProtocolCode(updated.getProtocolCode());
        }
        if (updated.getDiagnosis() != null) {
            existing.setDiagnosis(updated.getDiagnosis());
        }
        if (updated.getTotalCycles() != null) {
            existing.setTotalCycles(updated.getTotalCycles());
        }
        if (updated.getCurrentCycle() != null) {
            existing.setCurrentCycle(updated.getCurrentCycle());
        }
        if (updated.getCycleIntervalDays() != null) {
            existing.setCycleIntervalDays(updated.getCycleIntervalDays());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getStartDate() != null) {
            existing.setStartDate(updated.getStartDate());
        }
        if (updated.getEndDate() != null) {
            existing.setEndDate(updated.getEndDate());
        }
        if (updated.getNextDueDate() != null) {
            existing.setNextDueDate(updated.getNextDueDate());
        }
        if (updated.getPrescribedBy() != null) {
            existing.setPrescribedBy(updated.getPrescribedBy());
        }
        if (updated.getNotes() != null) {
            existing.setNotes(updated.getNotes());
        }
        if (updated.getUnitId() != null) {
            existing.setUnitId(updated.getUnitId());
        }
        return repository.save(existing);
    }

    public ChemoRegimen advanceCycle(Long id) {
        ChemoRegimen regimen = getById(id);
        int nextCycle = regimen.getCurrentCycle() + 1;
        regimen.setCurrentCycle(nextCycle);
        if (nextCycle >= regimen.getTotalCycles()) {
            regimen.setStatus(ChemoRegimen.STATUS_COMPLETED);
        } else {
            regimen.setStatus(ChemoRegimen.STATUS_ACTIVE);
        }
        if (regimen.getCycleIntervalDays() != null && regimen.getCycleIntervalDays() > 0) {
            LocalDate baseDate = regimen.getNextDueDate() != null ? regimen.getNextDueDate() : LocalDate.now();
            regimen.setNextDueDate(baseDate.plusDays(regimen.getCycleIntervalDays()));
        }
        return repository.save(regimen);
    }

    public ChemoRegimen updateStatus(Long id, String status) {
        ChemoRegimen regimen = getById(id);
        regimen.setStatus(status);
        return repository.save(regimen);
    }

    public void deleteRegimen(Long id) {
        repository.deleteById(id);
    }
}
