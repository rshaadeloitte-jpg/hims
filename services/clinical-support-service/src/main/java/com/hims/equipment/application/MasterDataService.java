package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentChecklist;
import com.hims.equipment.domain.repository.EquipmentMasterRepository;
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
import java.util.Set;

/**
 * Generic master-data use cases for the 13 V1 equipment master tables
 * (MIG-023 Task 17) — equipment-service port of the reference-service master
 * pattern (MasterRead/WriteUseCase, legacy GenericServiceImpl crudadd /
 * crudupdate / crudremove parity) adapted to house conventions:
 *
 * <ul>
 *   <li>server-owned ids drawn from each table's explicit sequence, burned
 *       only after payload validation passes (bind failures never spend ids);</li>
 *   <li>client-supplied {@code id} rejected (a supplied id could overwrite an
 *       existing dictionary row via merge — reference-service F-3 hardening);</li>
 *   <li>{@code id}/{@code unitId}/audit block/{@code synchronizedd} are
 *       server-owned on update (house skip-list, WarrantyService precedent)
 *       while {@code status} stays patchable so masters can be deactivated
 *       through PUT as well as DELETE;</li>
 *   <li>DELETE is a soft delete ({@code status=false}): seeded dictionary rows
 *       are referenced by code/id across the domain (complaint_status,
 *       working_status, maintenance_type), so they must never vanish.</li>
 * </ul>
 */
@Service
public class MasterDataService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    /** equipment_checklist.quantity column DEFAULT (applied on key-absence). */
    public static final long DEFAULT_CHECKLIST_QUANTITY = 1L;

    private final MasterEntityRegistry registry;
    private final EquipmentMasterRepository repository;
    private final ObjectMapper objectMapper;

    public MasterDataService(MasterEntityRegistry registry,
                             EquipmentMasterRepository repository,
                             ObjectMapper objectMapper) {
        this.registry = registry;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- reads

    /** Supported masterType path values (new contract surface, like reference-service). */
    public Set<String> supportedTypes() {
        return registry.names();
    }

    /**
     * Master listing: ordered by id (dictionary seed order). Defaults to active
     * rows only; pass {@code status=false} for deactivated entries or omit the
     * filter entirely by passing {@code null}. Optional unit gate + free-text
     * search over the registry whitelist.
     */
    @Transactional(readOnly = true)
    public List<EquipmentBase> list(String masterType, Long unitId, Boolean status, String search) {
        MasterEntityRegistry.MasterDef def = registry.require(masterType);
        String term = normalizeSearch(search);
        List<? extends EquipmentBase> rows =
                repository.search(def.entityClass(), unitId, status, def.searchFields(), term);
        log.debug("Master {} list: {} row(s) (unitId={}, status={}, search={})",
                def.name(), rows.size(), unitId, status, term);
        return List.copyOf(rows);
    }

    /** Single master row by id; unknown id -> 404 NOT_FOUND. */
    @Transactional(readOnly = true)
    public EquipmentBase get(String masterType, long id) {
        MasterEntityRegistry.MasterDef def = registry.require(masterType);
        requirePositiveId(id);
        return repository.findById(def.entityClass(), id)
                .orElseThrow(() -> notFound(def.name(), id));
    }

    // --------------------------------------------------------------- writes

    /**
     * Create a master row: validate (bind, required fields) BEFORE burning the
     * sequence, default {@code status=true}, apply the
     * equipment_checklist.quantity column default on key-absence, stamp audit
     * columns and persist.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentBase create(String masterType, JsonNode payload, Long unitId) {
        MasterEntityRegistry.MasterDef def = registry.require(masterType);
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentBase entity = bind(payload, def.entityClass());
        if (entity.getId() != null && entity.getId() > 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "id is server-assigned; do not supply it");
        }
        trimStrings(entity);
        applyColumnDefaults(payload, entity);
        validateRequired(def, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(true);
        }
        entity.setId(repository.nextId(def.sequenceName()));
        stampCreate(entity, unitId);
        EquipmentBase saved = repository.save(entity);
        log.info("Master {} created id={} unitId={}", def.name(), saved.getId(), unitId);
        return saved;
    }

    /**
     * Update a master row (null patch fields ignored; id/unitId/audit/
     * synchronizedd are server-owned). A blank string in the patch normalizes
     * to null first, so it cannot wipe a required business field.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentBase update(String masterType, long id, JsonNode payload) {
        MasterEntityRegistry.MasterDef def = registry.require(masterType);
        requirePositiveId(id);
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentBase existing = repository.findById(def.entityClass(), id)
                .orElseThrow(() -> notFound(def.name(), id));
        EquipmentBase patch = bind(payload, def.entityClass());
        trimStrings(patch);
        applyUpdate(payload, existing, patch);
        validateRequired(def, existing);
        stampUpdate(existing);
        EquipmentBase saved = repository.save(existing);
        log.info("Master {} updated id={}", def.name(), saved.getId());
        return saved;
    }

    /**
     * Soft delete: flips {@code status} to false and stamps the update audit.
     * Dictionary codes stay referentially intact (complaint_status /
     * working_status / maintenance_type ids ARE API codes).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentBase deactivate(String masterType, long id) {
        MasterEntityRegistry.MasterDef def = registry.require(masterType);
        requirePositiveId(id);
        EquipmentBase existing = repository.findById(def.entityClass(), id)
                .orElseThrow(() -> notFound(def.name(), id));
        existing.setStatus(false);
        stampUpdate(existing);
        EquipmentBase saved = repository.save(existing);
        log.info("Master {} deactivated id={}", def.name(), saved.getId());
        return saved;
    }

    // -------------------------------------------------------------- helpers

    /**
     * Column defaults that cannot ride Jackson binding. equipment_checklist.quantity
     * carries the DB DEFAULT 1 — applied here when the payload omits the key.
     * note #17 gotcha: detect key ABSENCE via {@code hasNonNull} rather than a null
     * field check, and keep the entity free of Java initializers so both paths
     * (key absent vs explicit value) stay distinguishable.
     */
    private void applyColumnDefaults(JsonNode payload, EquipmentBase entity) {
        if (entity instanceof EquipmentChecklist checklist) {
            if (!payload.hasNonNull("quantity") && checklist.getQuantity() == null) {
                checklist.setQuantity(DEFAULT_CHECKLIST_QUANTITY);
            }
        }
    }

    private EquipmentNotFoundException notFound(String masterType, long id) {
        return new EquipmentNotFoundException("NOT_FOUND",
                "Master record not found: " + masterType + "/" + id);
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    private <T extends EquipmentBase> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    /** Registry-driven required-field validation (blank/null -> 400 BEXA003). */
    private void validateRequired(MasterEntityRegistry.MasterDef def, EquipmentBase entity) {
        for (String required : def.requiredFields()) {
            Object value = fieldValue(entity, required);
            if (value == null || (value instanceof String s && s.isBlank())) {
                throw new InvalidEquipmentRequestException("BEXA003",
                        "Missing required field '" + required + "' for " + def.name());
            }
        }
    }

    /** Trim every String field; blank becomes null (never stored as whitespace). */
    private void trimStrings(EquipmentBase entity) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)
                        || field.getType() != String.class) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(entity);
                    if (value != null) {
                        field.set(entity, value.isBlank() ? null : value.trim());
                    }
                } catch (IllegalAccessException e) {
                    log.warn("Cannot normalize field {} on {}", field.getName(), entity.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Null-safe field copy from the patch onto the managed entity (waste-service
     * pattern). Skip list keeps id/unitId/audit/synchronizedd server-owned;
     * status stays patchable (deactivate/reactivate through PUT).
     *
     * <p>note #17 gotcha, fixed here: {@link EquipmentBase#status} carries a Java
     * initializer ({@code = true}), so Jackson leaves {@code true} on every bound
     * patch whose payload omits the key — a plain null-check copy silently
     * RE-ACTIVATED deactivated dictionary rows on unrelated business patches.
     * Fields are therefore only copied when the payload actually carries them as
     * keys ({@code payload.has(fieldName)}), giving exact PATCH semantics: an
     * absent key never changes the stored value, whatever the bound object holds.
     * Entity JSON names equal Java field names throughout the master model
     * (field-visibility auto-detection, no {@code @JsonProperty} renames), so the
     * name match is safe by construction.</p>
     */
    private void applyUpdate(JsonNode payload, EquipmentBase existing, EquipmentBase patch) {
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
                if (!payload.has(field.getName())) {
                    continue;   // key absent -> initializer/defaulted value must NOT leak through
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

    /** Reflection read of a field across the entity hierarchy (registry-required check). */
    private Object fieldValue(EquipmentBase entity, String name) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(entity);
            } catch (NoSuchFieldException e) {
                // walk up the hierarchy
            } catch (IllegalAccessException e) {
                log.warn("Cannot read field {} for validation", name);
                return null;
            }
        }
        return null;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String term = search.trim();
        return term.isEmpty() ? null : term;
    }

    private void stampCreate(EquipmentBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        entity.setCreatedUnitId(unitId);
        entity.setUpdatedUnitId(unitId);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampUpdate(EquipmentBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}
