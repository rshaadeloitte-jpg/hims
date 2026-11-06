package com.hims.grievance.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.application.exceptions.InvalidGrievanceRequestException;
import com.hims.grievance.domain.model.GrievanceBase;
import com.hims.grievance.domain.model.GrievanceCategory;
import com.hims.grievance.domain.model.GrievanceSeverity;
import com.hims.grievance.domain.model.GrievanceSubCategory;
import com.hims.grievance.domain.repository.GrievanceCategoryRepository;
import com.hims.grievance.domain.repository.GrievanceSeverityRepository;
import com.hims.grievance.domain.repository.GrievanceSubCategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Grievance classification masters write path (MIG-020). Same contract as the
 * reference/item masters write path: server-owned ids (per-table sequence) and
 * audit fields, required-field validation. REST-only (no outbox — GrievanceRaised
 * rejected phase 1, KAFKA-EVENT-CATALOG).
 */
@Service
public class GrievanceMasterService {

    private static final Logger log = LoggerFactory.getLogger(GrievanceMasterService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final GrievanceCategoryRepository categoryRepository;
    private final GrievanceSubCategoryRepository subCategoryRepository;
    private final GrievanceSeverityRepository severityRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public GrievanceMasterService(GrievanceCategoryRepository categoryRepository,
                                  GrievanceSubCategoryRepository subCategoryRepository,
                                  GrievanceSeverityRepository severityRepository,
                                  ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.severityRepository = severityRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------- category

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceCategory createCategory(JsonNode payload, Long unitId) {
        GrievanceCategory c = bind(payload, GrievanceCategory.class);
        require(c, "description");
        c.setId(nextId("grievance_category_seq"));
        c.setStatus(c.getStatus() == null ? true : c.getStatus());
        stampCreate(c, unitId);
        return categoryRepository.save(c);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceCategory updateCategory(Long id, JsonNode payload) {
        GrievanceCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("BEXA031", "Not Authorized for editing !"));
        GrievanceCategory patch = bind(payload, GrievanceCategory.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return categoryRepository.save(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceCategory removeCategory(Long id) {
        GrievanceCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
        categoryRepository.delete(existing);
        return existing;
    }

    // --------------------------------------------------------- sub-category

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSubCategory createSubCategory(JsonNode payload, Long unitId) {
        GrievanceSubCategory c = bind(payload, GrievanceSubCategory.class);
        require(c, "categoryId", "description");
        if (!categoryRepository.existsById(c.getCategoryId())) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        c.setId(nextId("grievance_sub_category_seq"));
        c.setStatus(c.getStatus() == null ? true : c.getStatus());
        stampCreate(c, unitId);
        return subCategoryRepository.save(c);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSubCategory updateSubCategory(Long id, JsonNode payload) {
        GrievanceSubCategory existing = subCategoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("BEXA031", "Not Authorized for editing !"));
        GrievanceSubCategory patch = bind(payload, GrievanceSubCategory.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return subCategoryRepository.save(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSubCategory removeSubCategory(Long id) {
        GrievanceSubCategory existing = subCategoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
        subCategoryRepository.delete(existing);
        return existing;
    }

    // ------------------------------------------------------------- severity

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSeverity createSeverity(JsonNode payload, Long unitId) {
        GrievanceSeverity c = bind(payload, GrievanceSeverity.class);
        require(c, "description");
        c.setId(nextId("grievance_severity_seq"));
        c.setStatus(c.getStatus() == null ? true : c.getStatus());
        stampCreate(c, unitId);
        return severityRepository.save(c);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSeverity updateSeverity(Long id, JsonNode payload) {
        GrievanceSeverity existing = severityRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("BEXA031", "Not Authorized for editing !"));
        GrievanceSeverity patch = bind(payload, GrievanceSeverity.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return severityRepository.save(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GrievanceSeverity removeSeverity(Long id) {
        GrievanceSeverity existing = severityRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
        severityRepository.delete(existing);
        return existing;
    }

    // -------------------------------------------------------------- helpers

    private <T extends GrievanceBase> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void require(GrievanceBase entity, String... fields) {
        for (String f : fields) {
            Object v = fieldValue(entity, f);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidGrievanceRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    private Object fieldValue(GrievanceBase entity, String field) {
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(field);
                    f.setAccessible(true);
                    return f.get(entity);
                } catch (NoSuchFieldException e) {
                    // continue up the hierarchy
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Cannot read field {} for validation", field);
        }
        return null;
    }

    private void stampCreate(GrievanceBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    /** Server-owned field copy (identity + audit columns never client-writable). */
    private void applyUpdate(GrievanceBase existing, GrievanceBase patch) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        || field.getName().equals("id") || field.getName().equals("unitId")
                        || field.getName().equals("addedBy") || field.getName().equals("addedDatetime")
                        || field.getName().equals("updatedBy") || field.getName().equals("updatedDatetime")
                        || field.getName().equals("createdUnitId") || field.getName().equals("updatedUnitId")
                        || field.getName().equals("synchronizedd")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(patch);
                    if (value != null) {
                        field.set(existing, value);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Cannot copy field {} on update", field.getName());
                }
            }
        }
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_grievance.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
