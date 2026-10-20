package com.hims.reference.application;

import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.repository.MasterReadRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read use cases for master entities, faithfully porting GenericServiceImpl
 * crudget / crudgetAll / crudgetAllAll semantics (see JpaMasterReadRepository).
 * Field names (search/order/filter) are validated against the entity's persisted
 * fields — legacy interpolated them raw into JPQL (IMP-DEC-010 hardening).
 */
@Service
public class MasterReadUseCase {

    private final MasterEntityRegistry registry;
    private final MasterReadRepository repository;

    public MasterReadUseCase(MasterEntityRegistry registry, MasterReadRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    /** Legacy crudget: GET /{entity}/{id} (id + unitId scope; Unitmaster is id-only). */
    public MasterEntityBase getById(String entityName, Long id, Long unitId, Map<String, String> filters) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        if (id == null || id <= 0) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        Map<String, Object> allowed = toFilterMap(def, filters);
        return repository.findByIdScoped(def.entityClass(), id, unitId, allowed)
                .orElseThrow(() -> new MasterNotFoundException("NOT_FOUND", "Record not found"));
    }

    /** Legacy crudgetAll: GET /{entity} — unit-scoped, search, filters, pagination. */
    public List<? extends MasterEntityBase> getScoped(String entityName, Long unitId, Map<String, String> filters,
                                                      String search, String searchFld, int page, int count) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        // Geo reference masters (D-6) are list-global: unitId is optional for them.
        // Unitmaster stays unit-scoped on the LIST surface (legacy crudgetAll parity);
        // only its single-read/write path is id-only (MasterScope, single source of truth).
        if (unitId == null && !com.hims.reference.domain.model.MasterScope.isListGlobal(def.entityClass())) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        SearchTerms terms = parseSearch(search, searchFld, true);
        validateSearchFields(def, terms, true);
        // Legacy GenericServiceImpl divides page by count, then GenericQuery.paginate
        // multiplies start*count again -> effective offset = (page / count) * count
        // (evidence: crudgetAll internalCrudgetAll + GenericQuery.paginate). S0 originally
        // used page/count (parity bug, caught by AGT-10 red team) — corrected 2026-08-17.
        int offset = count > 0 ? (page / count) * count : 0;
        return repository.findScoped(def.entityClass(), unitId, toFilterMap(def, filters),
                terms.fields(), terms.terms(), offset, count, terms.orderField());
    }

    /** Legacy crudgetAllAll: GET /{entity}/all — unscoped, single search term, filters, pagination. */
    public List<? extends MasterEntityBase> getAll(String entityName, Map<String, String> filters,
                                                   String search, String searchFld, int page, int count) {
        MasterEntityRegistry.MasterDef def = registry.require(entityName);
        SearchTerms terms = parseSearch(search, searchFld, false);
        validateSearchFields(def, terms, false);
        int offset = count > 0 ? (page / count) * count : 0;   // legacy (page/count)*count
        return repository.findAll(def.entityClass(), toFilterMap(def, filters),
                terms.fields(), terms.singleTerm(), offset, count, terms.orderField());
    }

    /**
     * JPQL injection hardening (AGT-10 F-2 / IMP-DEC-023): search/order fields are
     * validated against the entity's persisted fields before reaching the repository
     * (400 BEXA003). Legacy interpolated searchFld raw (GenericServiceImpl:217).
     */
    private void validateSearchFields(MasterEntityRegistry.MasterDef def, SearchTerms terms, boolean scoped) {
        if (terms.orderField() != null && !terms.orderField().isBlank()
                && !isPersistedField(def.entityClass(), terms.orderField().trim())) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        for (String f : terms.fields()) {
            if (f.isBlank() || f.equals("addeddatetime") || isPersistedField(def.entityClass(), f.trim())) {
                continue;
            }
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        if (terms.singleTerm() != null && terms.fields() != null && terms.fields().size() == 1
                && terms.fields().get(0).equalsIgnoreCase("id")) {
            try {
                Long.parseLong(terms.singleTerm().trim());
            } catch (NumberFormatException e) {
                throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
            }
        }
    }

    /** True when the (possibly inherited) field exists on the entity and is not @Transient. */
    private boolean isPersistedField(Class<? extends com.hims.reference.domain.model.MasterEntityBase> entityClass, String field) {
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(field);
                return f.getAnnotation(jakarta.persistence.Transient.class) == null;
            } catch (NoSuchFieldException e) {
                // continue up the hierarchy
            }
        }
        return false;
    }

    private Map<String, Object> toFilterMap(MasterEntityRegistry.MasterDef def, Map<String, String> filters) {
        Map<String, Object> result = new HashMap<>();
        if (filters == null) {
            return result;
        }
        for (Map.Entry<String, String> e : filters.entrySet()) {
            if (!def.allowedFilters().contains(e.getKey())) {
                continue;
            }
            String value = e.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            result.put(e.getKey(), coerceFilterValue(def.entityClass(), e.getKey(), value));
        }
        return result;
    }

    /**
     * Type-aware filter binding (MIG-011 follow-up, same class of fix as IMP-DEC-014
     * Timestamp binding): Hibernate will NOT coerce String -> Long/Boolean on the SQM
     * path, so FK filters (geo countryId/stateId/districtId/cityId, doctor
     * doctorTypeId/specializationId, district zoneid) must be bound with the entity
     * field's actual type. Field type resolved by reflection over the entity class
     * hierarchy; unknown fields fall back to String (whitelist already validated).
     */
    private Object coerceFilterValue(Class<? extends MasterEntityBase> entityClass, String field, String value) {
        Class<?> type = persistedFieldType(entityClass, field);
        try {
            if (type == Boolean.class || type == boolean.class) {
                return Boolean.valueOf(value);
            }
            if (type == Long.class || type == long.class) {
                return Long.valueOf(value.trim());
            }
            if (type == Integer.class || type == int.class) {
                return Integer.valueOf(value.trim());
            }
        } catch (NumberFormatException ex) {
            throw new InvalidMasterRequestException("BEXA003", "Invalid input data !");
        }
        return value;
    }

    private Class<?> persistedFieldType(Class<? extends MasterEntityBase> entityClass, String field) {
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(field).getType();
            } catch (NoSuchFieldException ignored) {
                // walk up the hierarchy (MasterEntityBase holds the audit block)
            }
        }
        return String.class;
    }

    /**
     * Legacy search semantics: searchFld is a space-separated field list; search is
     * comma-separated terms aligned positionally (crudgetAll) or a single term (all).
     */
    private SearchTerms parseSearch(String search, String searchFld, boolean commaAligned) {
        if (search == null || search.isBlank()) {
            return new SearchTerms(List.of(), List.of(), searchFld);
        }
        List<String> fields = new ArrayList<>();
        if (searchFld != null && !searchFld.isBlank()) {
            for (String f : searchFld.trim().split("\\s+")) {
                if (!f.isBlank()) {
                    fields.add(f);
                }
            }
        }
        List<String> terms = new ArrayList<>();
        if (commaAligned) {
            for (String t : search.split(",")) {
                if (!t.isBlank()) {
                    terms.add(t.trim());
                }
            }
        } else {
            terms.add(search.trim());
        }
        return new SearchTerms(fields, terms, searchFld);
    }

    private record SearchTerms(List<String> fields, List<String> terms, String orderField) {
        String singleTerm() {
            return terms.isEmpty() ? null : terms.get(0);
        }
    }
}
