package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** Events still due for dispatch: PENDING/FAILED below attempt cap, oldest first. */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status IN (com.hims.reference.domain.model.OutboxEvent.OutboxStatus.PENDING,
                               com.hims.reference.domain.model.OutboxEvent.OutboxStatus.FAILED)
              AND e.attempts < :maxAttempts
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<OutboxEvent> findDue(@Param("maxAttempts") int maxAttempts, Pageable pageable);

    /** Compare-and-set claim so concurrent relays cannot double-publish. */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.attempts = :attempts
            WHERE e.id = :id
              AND e.status IN (com.hims.reference.domain.model.OutboxEvent.OutboxStatus.PENDING,
                               com.hims.reference.domain.model.OutboxEvent.OutboxStatus.FAILED)
            """)
    int claim(@Param("id") long id, @Param("attempts") int attempts);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = com.hims.reference.domain.model.OutboxEvent.OutboxStatus.PUBLISHED,
                e.publishedAt = :publishedAt
            WHERE e.id = :id
            """)
    int markPublished(@Param("id") long id, @Param("publishedAt") Timestamp publishedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = :status
            WHERE e.id = :id
            """)
    int markFailed(@Param("id") long id, @Param("attempts") int attempts,
                   @Param("status") OutboxEvent.OutboxStatus status);
}
