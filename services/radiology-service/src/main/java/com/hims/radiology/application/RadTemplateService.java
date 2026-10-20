package com.hims.radiology.application;

import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.application.exceptions.RadOrderNotFoundException;
import com.hims.radiology.domain.model.RadTemplate;
import com.hims.radiology.domain.repository.RadTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class RadTemplateService {
    private static final long ACTOR = 1L;
    private final RadTemplateRepository repository;
    public RadTemplateService(RadTemplateRepository repository) { this.repository = repository; }

    @Transactional public RadTemplate create(String code, String modality, String body, String description) {
        if (code == null || code.isBlank()) throw new InvalidRadOrderException("BEXA003", "code is required");
        if (modality == null || modality.isBlank()) throw new InvalidRadOrderException("BEXA003", "modality is required");
        if (body == null || body.isBlank()) throw new InvalidRadOrderException("BEXA003", "body is required");
        if (repository.findByTemplateCode(code).isPresent()) throw new InvalidRadOrderException("RAD_DUPLICATE_TEMPLATE", "template code already exists");
        long id = repository.nextId();
        RadTemplate t = new RadTemplate();
        t.setId(id); t.setTemplateCode(code); t.setModality(modality); t.setBodyTemplate(body); t.setDescription(description);
        t.setAddedBy(ACTOR); t.setAddedDatetime(Timestamp.from(Instant.now()));
        t.setSynchronizedd(false);
        return repository.save(t);
    }

    @Transactional(readOnly = true) public RadTemplate get(long id) {
        return repository.findById(id).orElseThrow(() -> new RadOrderNotFoundException("NOT_FOUND", "Template not found: " + id));
    }
    @Transactional(readOnly = true) public List<RadTemplate> list(String modality) {
        if (modality != null) return repository.findByModality(modality);
        return repository.findAll();
    }
    private void stampUpdate(com.hims.radiology.domain.model.RadOrderBase entity) {
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}
