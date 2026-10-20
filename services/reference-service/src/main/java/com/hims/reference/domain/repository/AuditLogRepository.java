package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :t AND a.entityId = :id ORDER BY a.occurredAt DESC")
    List<AuditLog> findByEntity(@Param("t") String entityType, @Param("id") Long entityId);
    @Query("SELECT a FROM AuditLog a WHERE a.actorId = :actor ORDER BY a.occurredAt DESC")
    List<AuditLog> findByActor(@Param("actor") Long actorId);
    @Query(value = "SELECT nextval('ihms_master.audit_log_seq')", nativeQuery = true)
    long nextId();
}
