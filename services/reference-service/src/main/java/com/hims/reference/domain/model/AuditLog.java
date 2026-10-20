package com.hims.reference.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.sql.Timestamp;

@Entity
@Table(name = "audit_log")
public class AuditLog extends MasterEntityBase {
    @Id @Column(name = "id") private Long id;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    @Column(name = "entity_type", nullable = false, length = 64) private String entityType;
    public String getEntityType() { return entityType; } public void setEntityType(String v) { this.entityType = v; }
    @Column(name = "entity_id", nullable = false) private Long entityId;
    public Long getEntityId() { return entityId; } public void setEntityId(Long v) { this.entityId = v; }
    @Column(name = "action", nullable = false, length = 16) private String action;
    public String getAction() { return action; } public void setAction(String v) { this.action = v; }
    @Column(name = "actor_id") private Long actorId;
    public Long getActorId() { return actorId; } public void setActorId(Long v) { this.actorId = v; }
    @Column(name = "actor_username", length = 120) private String actorUsername;
    public String getActorUsername() { return actorUsername; } public void setActorUsername(String v) { this.actorUsername = v; }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff", columnDefinition = "jsonb") private String diff;
    public String getDiff() { return diff; } public void setDiff(String v) { this.diff = v; }
    @Column(name = "occurred_at", nullable = false) private Timestamp occurredAt;
    public Timestamp getOccurredAt() { return occurredAt; } public void setOccurredAt(Timestamp v) { this.occurredAt = v; }
}
