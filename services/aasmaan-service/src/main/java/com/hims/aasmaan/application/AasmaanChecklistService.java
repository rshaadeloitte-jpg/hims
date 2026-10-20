package com.hims.aasmaan.application;

import com.hims.aasmaan.domain.model.AasmaanChecklist;
import com.hims.aasmaan.domain.repository.AasmaanChecklistRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AasmaanChecklistService {
    private final AasmaanChecklistRepository repo;
    public AasmaanChecklistService(AasmaanChecklistRepository repo) { this.repo = repo; }

    public AasmaanChecklist save(AasmaanChecklist c) {
        c.setUpdatedOn(LocalDateTime.now());
        return repo.save(c);
    }
    public List<AasmaanChecklist> byCase(Long caseId) { return repo.findByCaseId(caseId); }
    public AasmaanChecklist byId(Long id) { return repo.findById(id).orElseThrow(); }
    public void delete(Long id) { repo.deleteById(id); }
}
