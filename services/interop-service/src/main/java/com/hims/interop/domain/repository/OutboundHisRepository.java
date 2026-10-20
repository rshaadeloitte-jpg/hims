package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.OutboundHis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboundHisRepository extends JpaRepository<OutboundHis, Long> {

    /** Ingress idempotency lookup: UNIQUE(event_type, aggregate_id). */
    Optional<OutboundHis> findByEventTypeAndAggregateId(String eventType, String aggregateId);

    boolean existsByEventTypeAndAggregateId(String eventType, String aggregateId);

    // Filter combinations are expressed as typed derived queries (not one
    // nullable-parameter JPQL query): Hibernate sends untyped null binds as
    // bytea on this schema, which PG rejects inside function calls.
    List<OutboundHis> findAllByOrderByIdDesc();

    List<OutboundHis> findByStatusOrderByIdDesc(Short status);

    List<OutboundHis> findByTargetSystemIgnoreCaseOrderByIdDesc(String target);

    List<OutboundHis> findByStatusAndTargetSystemIgnoreCaseOrderByIdDesc(Short status, String target);
}
