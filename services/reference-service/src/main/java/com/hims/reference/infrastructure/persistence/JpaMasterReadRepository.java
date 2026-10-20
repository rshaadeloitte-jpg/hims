package com.hims.reference.infrastructure.persistence;

import com.hims.reference.domain.model.MasterEntityBase;
import com.hims.reference.domain.repository.MasterReadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of the legacy generic read semantics.
 * <p>
 * Faithful port of GenericServiceImpl.crudget / crudgetAll / crudgetAllAll
 * (evidence: D:\ihms\src\main\java\com\ohum\services\GenericServiceImpl.java:107-339),
 * with one deliberate hardening: search/filter/order fields are validated against
 * the entity's persisted field set (JPQL injection was possible in legacy via
 * searchFld — not replicated; DECISIONS IMP-DEC-010).
 */
@Repository
public class JpaMasterReadRepository implements MasterReadRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<? extends MasterEntityBase> findByIdScoped(
            Class<? extends MasterEntityBase> entityClass, Long id, Long unitId, Map<String, Object> filters) {
        StringBuilder jpql = new StringBuilder("SELECT t FROM ").append(entityClass.getSimpleName())
                .append(" t WHERE t.id = :id");
        if (!com.hims.reference.domain.model.MasterScope.isIdOnlyScope(entityClass)) {
            jpql.append(" AND t.unitId = :unitId");
        }
        for (Map.Entry<String, Object> e : filters.entrySet()) {
            if (e.getKey().equals("id") || e.getKey().equals("unitId")) {
                continue;
            }
            jpql.append(" AND t.").append(e.getKey()).append(" = :").append(e.getKey());
        }
        TypedQuery<? extends MasterEntityBase> q = em.createQuery(jpql.toString(), entityClass)
                .setParameter("id", id);
        if (!com.hims.reference.domain.model.MasterScope.isIdOnlyScope(entityClass)) {
            q.setParameter("unitId", unitId);
        }
        filters.forEach((k, v) -> {
            if (!k.equals("id") && !k.equals("unitId")) {
                q.setParameter(k, v);
            }
        });
        List<? extends MasterEntityBase> result = q.setMaxResults(1).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<? extends MasterEntityBase> findScoped(
            Class<? extends MasterEntityBase> entityClass,
            Long unitId,
            Map<String, Object> filters,
            List<String> searchFields,
            List<String> searchTerms,
            int offset,
            int limit,
            String orderField) {
        assertSearchFields(entityClass, searchFields);
        assertOrderField(entityClass, orderField);
        // Geo reference masters (D-6) carry unit_id NULL and are listed without the
        // unitId predicate. Unitmaster stays unit-scoped on the list surface (legacy
        // crudgetAll parity) — MasterScope.isListGlobal is the single source of truth.
        boolean global = com.hims.reference.domain.model.MasterScope.isListGlobal(entityClass);
        StringBuilder jpql = new StringBuilder("SELECT t FROM ").append(entityClass.getSimpleName()).append(" t");
        boolean where = false;
        if (!global) {
            jpql.append(" WHERE t.unitId = :unitId");
            where = true;
        }

        for (Map.Entry<String, Object> e : filters.entrySet()) {
            if (e.getKey().equals("unitId")) {
                continue;
            }
            jpql.append(where ? " AND " : " WHERE ").append("t.").append(e.getKey()).append(" = :").append(e.getKey());
            where = true;
        }

        if (searchFields != null && searchFields.isEmpty() && searchTerms != null && !searchTerms.isEmpty()) {
            searchFields = List.of(defaultSearchField(entityClass));
        }
        if (searchFields != null && !searchFields.isEmpty() && searchTerms != null && !searchTerms.isEmpty() && !where) {
            // appendOrSearch emits AND-clauses; a bare global list+search needs a WHERE seed
            jpql.append(" WHERE 1 = 1");
            where = true;
        }
        appendOrSearch(jpql, entityClass, searchFields, searchTerms, true);

        if (orderField != null) {
            jpql.append(" ORDER BY t.").append(orderField).append(" ASC");
        }
        TypedQuery<? extends MasterEntityBase> q = em.createQuery(jpql.toString(), entityClass);
        if (!global) {
            q.setParameter("unitId", unitId);
        }
        filters.forEach((k, v) -> {
            if (!k.equals("unitId")) {
                q.setParameter(k, v);
            }
        });
        bindSearchParams(q, searchFields, searchTerms, true);
        if (limit > 0) {
            q.setFirstResult(offset).setMaxResults(limit);
        }
        return q.getResultList();
    }

    @Override
    public List<? extends MasterEntityBase> findAll(
            Class<? extends MasterEntityBase> entityClass,
            Map<String, Object> filters,
            List<String> searchFields,
            String searchTerm,
            int offset,
            int limit,
            String orderField) {
        assertSearchFields(entityClass, searchFields);
        assertOrderField(entityClass, orderField);
        StringBuilder jpql = new StringBuilder("SELECT t FROM ").append(entityClass.getSimpleName())
                .append(" t");

        if (filters != null && !filters.isEmpty()) {
            jpql.append(" WHERE ");
            boolean first = true;
            for (Map.Entry<String, Object> e : filters.entrySet()) {
                if (!first) {
                    jpql.append(" AND ");
                }
                jpql.append("t.").append(e.getKey()).append(" = :").append(e.getKey());
                first = false;
            }
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            jpql.append(filters == null || filters.isEmpty() ? " WHERE " : " AND (");
            if (searchFields != null && searchFields.size() == 1 && searchFields.get(0).equalsIgnoreCase("id")) {
                // bound parameter (was raw splice — injection vector fixed per AGT-10 F-2)
                jpql.append("t.id = :searchId");
            } else {
                List<String> fields = searchFields == null || searchFields.isEmpty()
                        ? List.of(defaultSearchField(entityClass)) : searchFields;
                boolean first = true;
                for (String fld : fields) {
                    if (!first) {
                        jpql.append(" OR ");
                    }
                    jpql.append("lower(t.").append(fld).append(") LIKE lower(:searchTerm)");
                    first = false;
                }
            }
            jpql.append(filters == null || filters.isEmpty() ? "" : ")");
        }

        if (orderField != null) {
            jpql.append(" ORDER BY t.").append(orderField).append(" ASC");
        }

        TypedQuery<? extends MasterEntityBase> q = em.createQuery(jpql.toString(), entityClass);
        if (filters != null) {
            filters.forEach(q::setParameter);
        }
        if (searchTerm != null && !searchTerm.isBlank()) {
            if (searchFields != null && searchFields.size() == 1 && searchFields.get(0).equalsIgnoreCase("id")) {
                q.setParameter("searchId", Long.parseLong(searchTerm.trim()));
            } else {
                q.setParameter("searchTerm", "%" + searchTerm + "%");
            }
        }
        if (limit > 0) {
            q.setFirstResult(offset).setMaxResults(limit);
        }
        return q.getResultList();
    }

    /**
     * JPQL injection hardening (AGT-10 F-2 / IMP-DEC-023): every search/order field is
     * validated against the entity's persisted fields BEFORE interpolation. Legacy
     * interpolated searchFld/orderField raw (CVE-class); the use case passes only
     * whitelisted tokens, this check is the defence-in-depth layer.
     */
    private void assertSearchFields(Class<? extends MasterEntityBase> entityClass, List<String> searchFields) {
        if (searchFields == null || searchFields.isEmpty()) {
            return;
        }
        for (String f : searchFields) {
            if (f.equals("addeddatetime") || f.isBlank()) {
                continue;   // legacy special-cases addeddatetime ranges; blank -> description
            }
            if (!isPersistedField(entityClass, f)) {
                throw new IllegalArgumentException("Unknown search field: " + f);
            }
        }
    }

    private void assertOrderField(Class<? extends MasterEntityBase> entityClass, String orderField) {
        if (orderField == null || orderField.isBlank()) {
            return;
        }
        if (!isPersistedField(entityClass, orderField)) {
            throw new IllegalArgumentException("Unknown order field: " + orderField);
        }
    }

    /**
     * Default search field when searchFld is blank: legacy defaulted to
     * {@code description}; clean geo/subunit masters carry {@code name} instead
     * (MIG-011 follow-up). Pick whichever the entity actually persists.
     */
    private String defaultSearchField(Class<? extends MasterEntityBase> entityClass) {
        return isPersistedField(entityClass, "description") ? "description"
                : isPersistedField(entityClass, "name") ? "name" : "description";
    }

    private boolean isPersistedField(Class<? extends MasterEntityBase> entityClass, String field) {
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(field.trim());
                return f.getAnnotation(jakarta.persistence.Transient.class) == null;
            } catch (NoSuchFieldException e) {
                // continue up the hierarchy
            }
        }
        return false;
    }

    /**
     * Legacy crudgetAll search: terms are comma-split and aligned positionally with the
     * space-split field list; each pair adds {@code lower(f) LIKE lower('%t%')} OR-ed
     * (GenericServiceImpl.java:182-228). {@code addeddatetime} is a range on {@code _}.
     */
    private void appendOrSearch(StringBuilder jpql, Class<? extends MasterEntityBase> entityClass,
                                List<String> fields, List<String> terms, boolean scoped) {
        if (fields == null || fields.isEmpty() || terms == null || terms.isEmpty()) {
            return;
        }
        // Legacy crudgetAll (GenericServiceImpl.internalCrudgetAll): addeddatetime range is
        // appended as TOP-LEVEL `and ... >= ... and ... <= ...` clauses (no parens); all other
        // search fields form ONE parenthesized OR group appended as `and (or-group)`.
        // S0 originally nested both inside a single paren group (precedence deviation, caught
        // by AGT-10 red team) — corrected 2026-08-17 to emit range clauses at top level.
        StringBuilder orGroup = new StringBuilder();
        boolean hasOr = false;
        for (int i = 0; i < fields.size(); i++) {
            String fld = fields.get(i);
            String term = terms.size() > i ? terms.get(i) : (terms.size() > 0 ? terms.get(0) : null);
            if (term == null || term.isBlank()) {
                continue;
            }
            if (fld.equals("addeddatetime")) {
                String[] range = term.split("_");
                if (range.length == 2 && !range[0].isBlank() && !range[1].isBlank()) {
                    jpql.append(" AND t.addeddatetime >= :from").append(i)
                            .append(" AND t.addeddatetime <= :to").append(i);
                } else if (range.length == 1 && !range[0].isBlank()) {
                    jpql.append(" AND t.addeddatetime >= :from").append(i);
                }
                continue;
            }
            if (fld.isBlank()) {
                fld = defaultSearchField(entityClass);
            }
            if (hasOr) {
                orGroup.append(" OR ");
            }
            orGroup.append("lower(t.").append(fld).append(") LIKE lower(:term").append(i).append(")");
            hasOr = true;
        }
        if (hasOr) {
            jpql.append(" AND (").append(orGroup).append(")");
        }
    }

    /**
     * Legacy crudgetAll range terms are date-or-datetime strings split on '_'
     * (e.g. '2026-01-01_2026-12-31'); PG compared them directly against the
     * TIMESTAMP column (implicit cast to midnight). Convert to java.sql.Timestamp
     * so Hibernate bound params behave identically.
     */
    private static java.sql.Timestamp legacyRangeTimestamp(String value) {
        String v = value.trim();
        try {
            return java.sql.Timestamp.valueOf(v);
        } catch (IllegalArgumentException e) {
            return java.sql.Timestamp.valueOf(v + " 00:00:00");
        }
    }

    @Override
    public MasterEntityBase save(MasterEntityBase entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    @Override
    public void delete(MasterEntityBase entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    @Override
    public long nextId(String sequenceName) {
        // Sequences live in ihms_master; names are quoted + case-preserved (IMP-DEC-007).
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_master.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }

    private void bindSearchParams(TypedQuery<?> q, List<String> fields, List<String> terms, boolean scoped) {
        // Mirrors appendOrSearch: bind exactly the parameters that were emitted.
        if (fields == null || fields.isEmpty() || terms == null || terms.isEmpty()) {
            return;
        }
        for (int i = 0; i < fields.size(); i++) {
            String term = terms.size() > i ? terms.get(i) : (terms.size() > 0 ? terms.get(0) : null);
            if (term == null || term.isBlank()) {
                continue;
            }
            if (fields.get(i).equals("addeddatetime")) {
                // Legacy interpolated raw literals (PG coerced 'YYYY-MM-DD' to timestamp
                // midnight); bound params need an explicit conversion (Hibernate will not
                // coerce String -> Timestamp on the SQM path).
                String[] range = term.split("_");
                if (range.length == 2 && !range[0].isBlank() && !range[1].isBlank()) {
                    q.setParameter("from" + i, legacyRangeTimestamp(range[0]));
                    q.setParameter("to" + i, legacyRangeTimestamp(range[1]));
                } else if (range.length == 1 && !range[0].isBlank()) {
                    q.setParameter("from" + i, legacyRangeTimestamp(range[0]));
                }
            } else {
                q.setParameter("term" + i, "%" + term + "%");
            }
        }
    }
}
