package com.hims.aasmaan.application;

import com.hims.aasmaan.domain.model.AasmaanCase;
import com.hims.aasmaan.domain.repository.AasmaanCaseRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class AasmaanCaseService {
    private final AasmaanCaseRepository repo;
    public AasmaanCaseService(AasmaanCaseRepository repo) { this.repo = repo; }

    public AasmaanCase create(AasmaanCase c) {
        if (c.getPatientId() == null || c.getCaseType() == null || c.getCaseType().isBlank()) {
            throw new IllegalArgumentException("patientId and caseType required");
        }
        c.setStatus((short) 1);
        c.setOpenedOn(LocalDate.now());
        return repo.save(c);
    }

    public List<AasmaanCase> findAll() { return repo.findAll(); }
    public AasmaanCase findById(Long id) { return repo.findById(id).orElseThrow(); }
    public AasmaanCase update(Long id, AasmaanCase update) {
        AasmaanCase existing = findById(id);
        existing.setCaseType(update.getCaseType());
        existing.setStatus(update.getStatus());
        if (update.getStatus() != null && update.getStatus() == 3 && existing.getClosedOn() == null) {
            existing.setClosedOn(LocalDate.now());
        }
        return repo.save(existing);
    }
    public void delete(Long id) { repo.deleteById(id); }
}
