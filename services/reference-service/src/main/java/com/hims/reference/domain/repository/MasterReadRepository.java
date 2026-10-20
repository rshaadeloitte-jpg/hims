package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.MasterEntityBase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read port replicating legacy GenericServiceImpl semantics
 * (crudget / crudgetAll / crudgetAllAll) for master entities.
 */
public interface MasterReadRepository {

    /**
     * Legacy crudget: id + unitId scope (Unitmaster is the documented exception —
     * id only, GenericServiceImpl.java:121-136).
     */
    Optional<? extends MasterEntityBase> findByIdScoped(
            Class<? extends MasterEntityBase> entityClass, Long id, Long unitId, Map<String, Object> filters);

    /**
     * Legacy crudgetAll (GET /{entity}): unitId scope + filters + search
     * (comma-split terms aligned per field) + pagination + orderBy searchFld.
     */
    List<? extends MasterEntityBase> findScoped(
            Class<? extends MasterEntityBase> entityClass,
            Long unitId,
            Map<String, Object> filters,
            List<String> searchFields,
            List<String> searchTerms,
            int offset,
            int limit,
            String orderField);

    /**
     * Legacy crudgetAllAll (GET /{entity}/all): no unit scope + filters + search
     * (single term OR'd across fields; id treated as equality) + pagination + orderBy.
     */
    List<? extends MasterEntityBase> findAll(
            Class<? extends MasterEntityBase> entityClass,
            Map<String, Object> filters,
            List<String> searchFields,
            String searchTerm,
            int offset,
            int limit,
            String orderField);

    /** MIG-010 write path: persist a master entity (persist or merge by id). */
    MasterEntityBase save(MasterEntityBase entity);

    /** MIG-010 write path: delete a master entity. */
    void delete(MasterEntityBase entity);

    /** MIG-010 write path: next value of the explicit per-table sequence (IMP-DEC-003). */
    long nextId(String sequenceName);
}
