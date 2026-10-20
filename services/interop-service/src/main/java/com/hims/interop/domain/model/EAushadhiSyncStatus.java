package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

/**
 * e-Aushadhi sync status history row (register WP-T1 extension).
 * One row appended per accepted transition.
 */
@Entity
@Table(name = "t_eaushadhi_sync_status")
public class EAushadhiSyncStatus extends InteropBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "sync_queue_id", nullable = false)
    private Long syncQueueId;

    @Column(name = "status_code", nullable = false)
    private Short statusCode;

    @Column(name = "ts", nullable = false)
    private Timestamp ts;

    @Column(name = "detail", length = 200)
    private String detail;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getSyncQueueId() { return syncQueueId; }
    public void setSyncQueueId(Long syncQueueId) { this.syncQueueId = syncQueueId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public Timestamp getTs() { return ts; }
    public void setTs(Timestamp ts) { this.ts = ts; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
