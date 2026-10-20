package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabDuplicateException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.ReportPrintTemplate;
import com.hims.lab.domain.repository.ReportPrintTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Report print template storage and retrieval (GAP-REGISTER WP-7 HIGH).
 * Manages 201 legacy print templates. Unifies M_PATHTEMPLATEMASTER (blob)
 * and M_PATHOTEMPLATEMASTER (text) into a single table.
 * Template types: REPORT, LABEL, RECEIPT, BARCODE.
 */
@Service
public class PrintTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PrintTemplateService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private static final List<String> VALID_TYPES = Arrays.asList("REPORT", "LABEL", "RECEIPT", "BARCODE");
    private static final List<String> VALID_CHANNELS = Arrays.asList("WHATSAPP", "SMS", "EMAIL", "CALL");

    private final ReportPrintTemplateRepository templateRepository;

    @PersistenceContext
    private EntityManager em;

    public PrintTemplateService(ReportPrintTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public ReportPrintTemplate create(String code, String description, String templateContent,
                                       byte[] templateBlob, Long genderId, Long serviceId,
                                       Long organId, Boolean isClinical, String templateType) {
        if (code == null || code.isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "code is required");
        }
        if (templateRepository.existsByCode(code.trim())) {
            throw new LabDuplicateException("LAB_REQ_DUPLICATE",
                    "Template with code '" + code + "' already exists");
        }
        String type = templateType == null ? "REPORT" : templateType.trim().toUpperCase();
        if (!VALID_TYPES.contains(type)) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "templateType must be one of " + VALID_TYPES);
        }

        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(nextId("t_report_print_template_seq"));
        t.setCode(code.trim());
        t.setDescription(description);
        t.setTemplateContent(templateContent);
        t.setTemplateBlob(templateBlob);
        t.setGenderId(genderId);
        t.setServiceId(serviceId);
        t.setOrganId(organId);
        t.setIsClinical(isClinical != null && isClinical);
        t.setTemplateType(type);
        t.setStatus(true);
        t.setSynchronizedd(false);
        t.setAddedBy(AUDIT_ACTOR_ID);
        t.setAddedDatetime(now());

        ReportPrintTemplate saved = templateRepository.save(t);
        log.info("Created print template {} (code={}, type={})", saved.getTemplateId(), code, type);
        return saved;
    }

    @Transactional
    public ReportPrintTemplate update(Long id, String description, String templateContent,
                                       byte[] templateBlob, Long genderId, Long serviceId,
                                       Long organId, Boolean isClinical, String templateType) {
        ReportPrintTemplate t = load(id);
        if (description != null) t.setDescription(description);
        if (templateContent != null) t.setTemplateContent(templateContent);
        if (templateBlob != null) t.setTemplateBlob(templateBlob);
        if (genderId != null) t.setGenderId(genderId);
        if (serviceId != null) t.setServiceId(serviceId);
        if (organId != null) t.setOrganId(organId);
        if (isClinical != null) t.setIsClinical(isClinical);
        if (templateType != null) {
            String type = templateType.trim().toUpperCase();
            if (!VALID_TYPES.contains(type)) {
                throw new InvalidLabRequestException("LAB_REQ_INVALID",
                        "templateType must be one of " + VALID_TYPES);
            }
            t.setTemplateType(type);
        }
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(now());
        ReportPrintTemplate saved = templateRepository.save(t);
        log.info("Updated print template {}", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public ReportPrintTemplate get(Long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public ReportPrintTemplate getByCode(String code) {
        return templateRepository.findByCode(code)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Template not found: " + code));
    }

    @Transactional(readOnly = true)
    public List<ReportPrintTemplate> listAll() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReportPrintTemplate> listByType(String templateType) {
        if (templateType == null || templateType.isBlank()) {
            return templateRepository.findAll();
        }
        String type = templateType.trim().toUpperCase();
        if (!VALID_TYPES.contains(type)) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "templateType must be one of " + VALID_TYPES);
        }
        return templateRepository.findByTemplateTypeOrderByCodeAsc(type);
    }

    @Transactional(readOnly = true)
    public List<ReportPrintTemplate> listByGender(Long genderId) {
        return templateRepository.findByGenderIdOrderByCodeAsc(genderId);
    }

    @Transactional(readOnly = true)
    public List<ReportPrintTemplate> listByService(Long serviceId) {
        return templateRepository.findByServiceIdOrderByCodeAsc(serviceId);
    }

    @Transactional
    public void delete(Long id) {
        ReportPrintTemplate t = load(id);
        templateRepository.delete(t);
        log.info("Deleted print template {}", id);
    }

    private ReportPrintTemplate load(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Print template not found: " + id));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_lab." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
