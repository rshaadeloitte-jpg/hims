package com.hims.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Ingest-idempotency ledger row (register WP-T2 STRICT idempotency requirement).
 * NOT in the register five-table list — added because accumulating read models
 * (rm_encounter_stats / rm_revenue_daily) cannot detect a re-ingested event from
 * their natural keys alone; DISCLOSED in WP-T2-reporting.md.
 * event_key is the deterministic source-document key: 'patient:&lt;id&gt;',
 * 'encounter:&lt;id&gt;', 'invoice:&lt;id&gt;', 'payment:&lt;id&gt;'.
 */
@Entity
@Table(name = "t_ingested_event")
public class IngestedEvent extends ReportingBase {

    @Id
    @Column(name = "ingested_id")
    private Long ingestedId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "ingested_at", nullable = false)
    private Timestamp ingestedAt;

    public Long getIngestedId() { return ingestedId; }
    public void setIngestedId(Long ingestedId) { this.ingestedId = ingestedId; }
    public Long getId() { return ingestedId; }
    public void setId(Long id) { this.ingestedId = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public Timestamp getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Timestamp ingestedAt) { this.ingestedAt = ingestedAt; }
}
