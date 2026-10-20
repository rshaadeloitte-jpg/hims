package com.hims.reference.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.repository.MasterReadRepository;
import com.hims.reference.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Master WRITE path (MIG-010, Wave 1). Ports legacy GenericServiceImpl
 * crudadd / crudupdate / crudremove semantics (evidence D:\ihms
 * GenericServiceImpl.java:36-105, MBase.update) with target-side hardening:
 * <ul>
 *   <li>unit scope enforced on update/remove for non-Unitmaster entities
 *       (legacy updated by id only — authz gap, not replicated; IMP-DEC-020)</li>
 *   <li>{@code nonNull} defaults to {@code true} (legacy default false = full
 *       overwrite incl. nulls; safer default, gateway can opt into legacy
 *       behaviour with {@code nonNull=false}; IMP-DEC-020)</li>
 *   <li>remove of a missing record returns 404 NOT_FOUND (legacy crashed with NPE;
 *       consistent with IMP-DEC-011 read deviation)</li>
 *   <li>every write appends a {@code MasterDataChanged} event to the transactional
 *       outbox (same DB tx — system prompt §26)</li>
 * </ul>
 * Audit actor id is a placeholder until MIG-004 IAM supplies the principal.
 */
@Service
public class MasterWriteUseCase {

    private static final Logger log = LoggerFactory.getLogger(MasterWriteUseCase.class);

    /** Placeholder audit actor until MIG-004 (dev basic-auth principal -> 1). */
    public static final long AUDIT_ACTOR_ID = 1L;

    public static final String EVENT_TYPE_MASTER_CHANGED = "MasterDataChanged";

    private final MasterEntityRegistry registry;
    private final MasterReadRepository repository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public MasterWriteUseCase(MasterEntityRegistry registry,
                              MasterReadRepository repository,
                              OutboxService outboxService,
                              ObjectMapper objectMapper) {
        this.registry = registry;
        this.repository = repository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    /** Legacy crudadd: validate -> save -> (audit/outbox). Returns the persisted entity. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // legacy TxType.REQUIRES_NEW
    public MasterEntityBase create(String entityName, JsonNode payload, Long unitId) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        MasterEntityBase entity = toEntity(def, payload);
        boolean unitScoped = isUnitScoped(def);
        // F-3 (AGT-10): ids are server-assigned from the per-table sequence — a
        // client-supplied id could overwrite existing rows via merge and inject
        // arbitrary ids into a legacy-referenced ID space.
        if (entity.getId() != null && entity.getId() > 0) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        entity.setId(repository.nextId(def.sequenceName()));
        // F-12 (AGT-10): registry-driven required fields (legacy relied on UI; new
        // services enforce server-side — evidence: master screens always send these).
        for (String required : def.requiredFields()) {
            Object v = fieldValue(entity, required);
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
            }
        }
        if (entity.getStatus() == null) {
            entity.setStatus(true);          // MBase default status = true
        }
        if (unitScoped) {
            Long scope = unitId != null ? unitId : entity.getUnitId();
            if (scope == null) {
                throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
            }
            entity.setUnitId(scope);
        }
        // audit fields are server-owned — never client-supplied (hardening; legacy
        // MBase callback stamped these server-side, payload values were overwritten)
        entity.setAddedby(AUDIT_ACTOR_ID);                       // MIG-004: principal id
        entity.setAddeddatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedby(null);
        entity.setUpdateddatetime(null);
        entity.setCreatedunitid(null);
        entity.setUpdatedunitid(null);
        entity.setSynchronizedd(false);
        repository.save(entity);
        emit(def, entity.getId(), "create");
        log.info("Master created entity={} id={}", def.name(), entity.getId());
        return entity;
    }

    /** Legacy crudupdate: id!=0 (BEXA003), missing record -> BEXA031, MBase.update field copy. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // legacy TxType.REQUIRES_NEW
    public MasterEntityBase update(String entityName, long id, JsonNode payload,
                                   boolean nonNull, Set<String> updFlds, Long unitId) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        if (id <= 0) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        MasterEntityBase existing = findExisting(def, id, unitId);
        if (existing == null) {
            // legacy code BEXA031 "Not Authorized for editing !" carried through; HTTP 404
            // per new-contract convention (IMP-DEC-011) — F-14 (AGT-10)
            throw new MasterNotFoundException("BEXA031", "Not Authorized for editing !");
        }
        MasterEntityBase patch = toEntity(def, payload);
        applyLegacyUpdate(existing, patch, nonNull, updFlds);
        existing.setUpdatedby(AUDIT_ACTOR_ID);
        existing.setUpdateddatetime(Timestamp.from(Instant.now()));
        repository.save(existing);
        emit(def, id, "update");
        log.info("Master updated entity={} id={}", def.name(), id);
        return existing;
    }

    /** Legacy crudremove: fetch by id, delete; missing -> 404 (documented deviation). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // legacy TxType.REQUIRES_NEW
    public MasterEntityBase remove(String entityName, long id, Long unitId) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        if (id <= 0) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        MasterEntityBase existing = findExisting(def, id, unitId);
        if (existing == null) {
            throw new MasterNotFoundException("NOT_FOUND", "Record not found");
        }
        repository.delete(existing);
        emit(def, id, "remove");
        log.info("Master removed entity={} id={}", def.name(), id);
        return existing;
    }

    // ------------------------------------------------------------------ helpers

    private boolean isUnitScoped(MasterEntityRegistry.MasterDef def) {
        return !com.hims.reference.domain.model.MasterScope.isIdOnlyScope(def.entityClass());
    }

    private MasterEntityBase findExisting(MasterEntityRegistry.MasterDef def, long id, Long unitId) {
        if (isUnitScoped(def)) {
            return repository.findByIdScoped(def.entityClass(), id, unitId, Map.of()).orElse(null);
        }
        return repository.findByIdScoped(def.entityClass(), id, null, Map.of()).orElse(null);
    }

    private MasterEntityBase toEntity(MasterEntityRegistry.MasterDef def, JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, def.entityClass());
        } catch (Exception e) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
    }

    /**
     * MBase.update parity (evidence MBase.java): copy every non-static/non-final
     * field from the patch unless {@code updFlds} restricts the set, and skip nulls
     * when {@code nonNull}. Hardening: id and unitId are never overwritten
     * (legacy forced unitId = existing before update; id came from the path).
     */
    private void applyLegacyUpdate(MasterEntityBase existing, MasterEntityBase patch,
                                   boolean nonNull, Set<String> updFlds) {
        for (Class<?> c = patch.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getName().startsWith("_persistence_")
                        // identity + audit/derived columns are server-owned (hardening):
                        // id, unitId, addedby, addeddatetime, updatedby, updateddatetime,
                        // synchronizedd, complete
                        || field.getName().equals("id") || field.getName().equals("unitId")
                        || field.getName().equals("addedby") || field.getName().equals("addeddatetime")
                        || field.getName().equals("updatedby") || field.getName().equals("updateddatetime")
                        || field.getName().equals("createdunitid") || field.getName().equals("updatedunitid")
                        || field.getName().equals("synchronizedd") || field.getName().equals("complete")) {
                    continue;
                }
                if (updFlds != null && !updFlds.contains(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(patch);
                    if (!nonNull || value != null) {
                        field.set(existing, value);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Cannot copy field {} on update", field.getName());
                }
            }
        }
    }

    /** Transactional outbox append (same tx as the business write — §26). */
    private void emit(MasterEntityRegistry.MasterDef def, long id, String op) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("masterType", def.name());
        payload.put("masterId", id);
        payload.put("op", op);
        try {
            outboxService.append(EVENT_TYPE_MASTER_CHANGED, def.entityClass().getSimpleName(), id,
                    String.valueOf(id), objectMapper.writeValueAsString(payload),
                    def.name() + ":" + id + ":" + op + ":" + UUID_placeholder());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    /** F-12/AGT-10: reflection read of a field (used for required-field checks). */
    private Object fieldValue(MasterEntityBase entity, String field) {
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField(field);
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

    private static String UUID_placeholder() {
        return java.util.UUID.randomUUID().toString();
    }
}
