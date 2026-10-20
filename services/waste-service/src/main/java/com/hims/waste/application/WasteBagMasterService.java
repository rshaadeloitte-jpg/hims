package com.hims.waste.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.waste.application.exceptions.InvalidWasteRequestException;
import com.hims.waste.application.exceptions.WasteNotFoundException;
import com.hims.waste.domain.model.WasteBagMaster;
import com.hims.waste.domain.model.WasteBase;
import com.hims.waste.domain.repository.WasteBagMasterRepository;
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
import java.util.List;

/**
 * Waste bag category master write path (MIG-023 sub-slice, S19). Clean target of
 * legacy M_WASTEBAGMASTER (@CrudApi allowedFilters=status). Same contract as the
 * reference/item/grievance masters write path: server-owned ids (per-table
 * sequence) and audit fields, required-field validation. REST-only (no outbox —
 * BMWRecorded rejected phase 1, KAFKA-EVENT-CATALOG).
 */
@Service
public class WasteBagMasterService {

    private static final Logger log = LoggerFactory.getLogger(WasteBagMasterService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final WasteBagMasterRepository bagRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public WasteBagMasterService(WasteBagMasterRepository bagRepository, ObjectMapper objectMapper) {
        this.bagRepository = bagRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WasteBagMaster createBag(JsonNode payload, Long unitId) {
        WasteBagMaster b = bind(payload, WasteBagMaster.class);
        require(b, "code", "name");
        if (unitId != null && bagRepository.existsByUnitIdAndCodeIgnoreCase(unitId, b.getCode())) {
            throw new InvalidWasteRequestException("BEXA003", "Bag code already exists for this unit");
        }
        b.setId(nextId("waste_bag_master_seq"));
        b.setStatus(b.getStatus() == null ? true : b.getStatus());
        stampCreate(b, unitId);
        WasteBagMaster saved = bagRepository.save(b);
        log.info("Waste bag master created id={} code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WasteBagMaster updateBag(Long id, JsonNode payload) {
        WasteBagMaster existing = bagRepository.findById(id)
                .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Waste bag master not found"));
        WasteBagMaster patch = bind(payload, WasteBagMaster.class);
        applyUpdate(existing, patch);
        existing.setUpdatedBy(AUDIT_ACTOR_ID);
        existing.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return bagRepository.save(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WasteBagMaster removeBag(Long id) {
        WasteBagMaster existing = bagRepository.findById(id)
                .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Waste bag master not found"));
        bagRepository.delete(existing);
        return existing;
    }

    @Transactional(readOnly = true)
    public List<WasteBagMaster> listBags(Long unitId) {
        return unitId == null
                ? bagRepository.findByStatusTrueOrderByIdAsc()
                : bagRepository.findByUnitIdAndStatusTrueOrderByIdAsc(unitId);
    }

    @Transactional(readOnly = true)
    public WasteBagMaster getBag(Long id) {
        return bagRepository.findById(id)
                .orElseThrow(() -> new WasteNotFoundException("NOT_FOUND", "Waste bag master not found"));
    }

    // -------------------------------------------------------------- helpers

    private <T extends WasteBase> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void require(WasteBase entity, String... fields) {
        for (String f : fields) {
            Object v = fieldValue(entity, f);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidWasteRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    private Object fieldValue(WasteBase entity, String field) {
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

    private void stampCreate(WasteBase entity, Long unitId) {
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
    private void applyUpdate(WasteBase existing, WasteBase patch) {
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
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_waste.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
