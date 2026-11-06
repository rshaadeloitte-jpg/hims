package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generic EntityManager-backed repository for the 13 V1 master tables
 * (MIG-023 Task 17). Mirrors what a Spring Data interface would generate per
 * entity, but one component serves all registered master types — the entity
 * class comes from {@code MasterEntityRegistry} and the only string fragments
 * interpolated into JPQL are registry-owned identifiers (entity simple name +
 * whitelisted search fields); every value is bound as a typed parameter.
 *
 * <p>Ids stay server-owned: assigned from the explicit per-table sequence via
 * native nextval before persist (IMP-DEC-003, no auto-increment).</p>
 */
@Repository
public class EquipmentMasterRepository {

    @PersistenceContext
    private EntityManager em;

    /** Persist (or re-attach) a master row. */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T extends EquipmentBase> T save(T entity) {
        return em.merge(entity);
    }

    public <T extends EquipmentBase> Optional<T> findById(Class<T> type, long id) {
        return Optional.ofNullable(em.find(type, id));
    }

    /**
     * Master listing: rows ordered by id (dictionary seed order), optional unit
     * gate, active/inactive filter and free-text LIKE over registry-whitelisted
     * fields. {@code status == null} means "no status filter".
     */
    public <T extends EquipmentBase> List<T> search(Class<T> type,
                                                    Long unitId,
                                                    Boolean status,
                                                    List<String> searchFields,
                                                    String term) {
        List<String> conditions = new ArrayList<>();
        if (status != null) {
            conditions.add(status ? "e.status = TRUE" : "e.status = FALSE");
        }
        if (unitId != null) {
            conditions.add("e.unitId = :unitId");
        }
        boolean hasTerm = term != null && !searchFields.isEmpty();
        if (hasTerm) {
            List<String> likes = new ArrayList<>();
            for (String field : searchFields) {
                likes.add("LOWER(e." + field + ") LIKE :term");
            }
            conditions.add("(" + String.join(" OR ", likes) + ")");
        }
        StringBuilder jpql = new StringBuilder("SELECT e FROM ")
                .append(type.getSimpleName()).append(" e");
        if (!conditions.isEmpty()) {
            jpql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        jpql.append(" ORDER BY e.id ASC");

        var query = em.createQuery(jpql.toString(), type);
        if (unitId != null) {
            query.setParameter("unitId", unitId);
        }
        if (hasTerm) {
            query.setParameter("term", "%" + term.toLowerCase() + "%");
        }
        return query.getResultList();
    }

    /** Next id from an explicit per-table sequence (native nextval). */
    public long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery(
                        "SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
