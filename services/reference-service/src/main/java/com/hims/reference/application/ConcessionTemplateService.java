package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.ConcessionTemplate;
import com.hims.reference.domain.repository.ConcessionTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class ConcessionTemplateService {
    private static final long ACTOR = 1L;
    private final ConcessionTemplateRepository repository;
    public ConcessionTemplateService(ConcessionTemplateRepository repository) { this.repository = repository; }

    @Transactional
    public ConcessionTemplate create(String code, String name, String scheme, String description,
                                    BigDecimal maxPercentOff, Boolean requiresApproval) {
        if (code == null || code.isBlank()) throw new InvalidMasterRequestException("BEXA003", "code is required");
        if (name == null || name.isBlank()) throw new InvalidMasterRequestException("BEXA003", "name is required");
        if (scheme == null || scheme.isBlank()) throw new InvalidMasterRequestException("BEXA003", "scheme is required");
        if (maxPercentOff == null || maxPercentOff.signum() < 0 || maxPercentOff.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidMasterRequestException("BEXA003", "maxPercentOff must be between 0 and 100");
        }
        long id = repository.nextId();
        ConcessionTemplate t = new ConcessionTemplate();
        t.setId(id); t.setCode(code); t.setName(name); t.setScheme(scheme);
        t.setDescription(description); t.setMaxPercentOff(maxPercentOff);
        t.setRequiresApproval(Boolean.TRUE.equals(requiresApproval));
        t.setDocStatus(ConcessionTemplate.STATUS_ACTIVE);
        stampCreate(t);
        return repository.save(t);
    }

    @Transactional
    public ConcessionTemplate deactivate(long id) {
        ConcessionTemplate t = get(id);
        t.setDocStatus(ConcessionTemplate.STATUS_INACTIVE);
        stampUpdate(t);
        return repository.save(t);
    }

    @Transactional(readOnly = true) public ConcessionTemplate get(long id) {
        return repository.findById(id).orElseThrow(() -> new MasterNotFoundException("NOT_FOUND", "Concession template not found: " + id));
    }
    @Transactional(readOnly = true) public List<ConcessionTemplate> list(String scheme, Short status) {
        if (scheme != null) return repository.findByScheme(scheme);
        if (status != null) return repository.findByDocStatus(status);
        return repository.findAll();
    }
    private void stampCreate(ConcessionTemplate t) {
        t.setAddedby(ACTOR); t.setAddeddatetime(Timestamp.from(Instant.now()));
        t.setUpdatedby(ACTOR); t.setUpdateddatetime(Timestamp.from(Instant.now()));
        t.setStatus(true); t.setSynchronizedd(false);
    }
    private void stampUpdate(ConcessionTemplate t) {
        t.setUpdatedby(ACTOR); t.setUpdateddatetime(Timestamp.from(Instant.now()));
    }
}
