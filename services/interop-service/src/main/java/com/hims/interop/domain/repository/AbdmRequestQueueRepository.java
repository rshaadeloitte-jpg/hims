package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.AbdmRequestQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AbdmRequestQueueRepository extends JpaRepository<AbdmRequestQueue, Long> {

    Optional<AbdmRequestQueue> findByCorrelationId(String correlationId);

    boolean existsByCorrelationId(String correlationId);

    /**
     * Producer-side dedup: a Kafka replay of an already-ingested event hits
     * the UNIQUE(envelope_event_id) index in the DB; the API check is the
     * fast path that translates a hit to a 409 envelope.
     */
    @Query("SELECT a FROM AbdmRequestQueue a WHERE a.envelopeEventId = :envelopeEventId")
    Optional<AbdmRequestQueue> findByEnvelopeEventId(@Param("envelopeEventId") String envelopeEventId);
}
