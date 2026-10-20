package com.hims.reference.domain.model;

import java.util.Set;

/**
 * Unit-scope policy for master entities (single source of truth).
 * <p>
 * Two distinct legacy behaviours are modelled:
 * <ul>
 *   <li><b>Id-only scope</b> (legacy {@code crudget} / single-read + write path): the
 *       unitId predicate is NOT applied. Legacy applied this only to Unitmaster
 *       (GenericServiceImpl.java:121-136). The clean target extends it to the geo
 *       reference family per D-6 (IMP-DEC-021) — geo rows carry unit_id NULL.</li>
 *   <li><b>List scope</b> (legacy {@code crudgetAll} / {@code findScoped}): the unitId
 *       predicate IS applied for every unit-scoped master. Legacy applied it to
 *       Unitmaster too (the generic list always filtered by unitId); only the new geo
 *       family is list-global, because geo reference data is shared across all units
 *       and has no owning unit.</li>
 * </ul>
 */
public final class MasterScope {

    private static final Set<String> GEO = Set.of(
            "GeoCountry", "GeoState", "GeoDistrict", "GeoCity", "GeoArea");

    private MasterScope() {
    }

    /** Geo reference family (D-6 global scope). */
    private static boolean isGeo(Class<? extends MasterEntityBase> entityClass) {
        return GEO.contains(entityClass.getSimpleName());
    }

    /**
     * True when the generic LIST (crudgetAll / findScoped) must NOT apply the unitId
     * predicate. Geo family only — Unitmaster stays unit-scoped on list (legacy parity).
     */
    public static boolean isListGlobal(Class<? extends MasterEntityBase> entityClass) {
        return isGeo(entityClass);
    }

    /**
     * True when the single-read (crudget / findByIdScoped) and the write path must NOT
     * apply the unitId predicate. Legacy Unitmaster exception + geo family (D-6).
     */
    public static boolean isIdOnlyScope(Class<? extends MasterEntityBase> entityClass) {
        return entityClass.getSimpleName().equals("UnitMaster") || isGeo(entityClass);
    }
}
