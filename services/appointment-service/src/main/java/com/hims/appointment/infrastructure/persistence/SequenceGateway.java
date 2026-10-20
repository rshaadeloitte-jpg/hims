package com.hims.appointment.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Server-owned explicit-sequence id assignment (IMP-DEC-003): ids are drawn
 * with SELECT nextval INSIDE the business transaction so rollback never burns
 * an id and the sequence is the single source of truth.
 */
@Component
public class SequenceGateway {

    @PersistenceContext
    private EntityManager em;

    public long next(String schemaQualifiedSequence) {
        Number n = (Number) em.createNativeQuery(
                        "SELECT nextval('" + schemaQualifiedSequence + "')")
                .getSingleResult();
        return n.longValue();
    }
}
