package com.hims.search.domain.model;

import com.hims.search.domain.model.SearchBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row of the t_index_request journal (register WP-T3): the DURABLE side of
 * the indexing pipeline — Solr document state itself lives in Solr.
 *
 * Status machine (SMALLINT): 0=PENDING -> dispatch -> 1=INDEXED | 2=FAILED;
 * retry guard FAILED-only (2 -> 0 -> dispatch again).
 * Action (SMALLINT): 1=INDEX 2=DELETE — DELETE is reserved for the encounter
 * retention/archive job (OUT OF SCOPE per 07 §1.7); no route creates it today.
 *
 * UNIQUE(collection, doc_id, action, status) is deliberately NOT enforced:
 * dedupe is via the latest-row query — an existing PENDING/INDEXED row makes a
 * duplicate ingress idempotent, while an existing FAILED row allows a fresh
 * PENDING attempt. Both behaviors are pinned by tests.
 */
@Entity
@Table(name = "t_index_request")
public class IndexRequest extends SearchBase {

    public static final short ACTION_INDEX = 1;
    public static final short ACTION_DELETE = 2;

    public static final short STATUS_PENDING = 0;
    public static final short STATUS_INDEXED = 1;
    public static final short STATUS_FAILED = 2;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "collection", nullable = false, length = 30)
    private String collection;

    @Column(name = "action", nullable = false)
    private Short action = ACTION_INDEX;

    @Column(name = "doc_id", nullable = false, length = 60)
    private String docId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public Short getAction() { return action; }
    public void setAction(Short action) { this.action = action; }
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isPending() { return status == STATUS_PENDING; }
    public boolean isIndexed() { return status == STATUS_INDEXED; }
    public boolean isFailed() { return status == STATUS_FAILED; }

    public static String statusName(short s) {
        return switch (s) {
            case STATUS_PENDING -> "PENDING";
            case STATUS_INDEXED -> "INDEXED";
            case STATUS_FAILED -> "FAILED";
            default -> String.valueOf(s);
        };
    }
}
