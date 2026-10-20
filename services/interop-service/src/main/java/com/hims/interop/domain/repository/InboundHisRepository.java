package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.InboundHis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InboundHisRepository extends JpaRepository<InboundHis, Long> {

    List<InboundHis> findAllByOrderByIdDesc();

    /**
     * In-flight continuity: a Kafka replay of an already-HTTP-ingested message
     * is a no-op. The query finds by envelope eventId (the outbox UUID used
     * for cross-service dedup). NULL envelope_event_id rows are NOT returned.
     */
    @Query("SELECT i FROM InboundHis i WHERE i.envelopeEventId = :envelopeEventId")
    Optional<InboundHis> findByEnvelopeEventId(@Param("envelopeEventId") String envelopeEventId);
}
