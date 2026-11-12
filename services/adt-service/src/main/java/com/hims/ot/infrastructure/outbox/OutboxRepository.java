package com.hims.ot.infrastructure.outbox;

import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claim due events: PENDING/FAILED below attempt cap whose backoff has elapsed,
     * oldest first, row-locked FOR UPDATE SKIP LOCKED (lock timeout -2) so parallel
     * relays split the backlog instead of contending. Call inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT e FROM OtOutboxEvent e
            WHERE e.status IN (com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.PENDING,
                               com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.FAILED)
              AND e.attempts < :maxAttempts
              AND e.nextAttemptAt <= :now
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<OutboxEvent> findDue(@Param("maxAttempts") int maxAttempts, @Param("now") Timestamp now,
                              Pageable pageable);

    /** Compare-and-set claim so concurrent relays cannot double-publish. */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OtOutboxEvent e
            SET e.attempts = :attempts,
                e.lockedAt = CURRENT_TIMESTAMP
            WHERE e.id = :id
              AND e.status IN (com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.PENDING,
                               com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.FAILED)
            """)
    int claim(@Param("id") long id, @Param("attempts") int attempts);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OtOutboxEvent e
            SET e.status = com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.PUBLISHED,
                e.publishedAt = :publishedAt
            WHERE e.id = :id
            """)
    int markPublished(@Param("id") long id, @Param("publishedAt") Timestamp publishedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OtOutboxEvent e
            SET e.status = com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.FAILED,
                e.attempts = :attempts,
                e.nextAttemptAt = :nextAttemptAt
            WHERE e.id = :id
            """)
    int markFailed(@Param("id") long id, @Param("attempts") int attempts,
                   @Param("nextAttemptAt") Timestamp nextAttemptAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE OtOutboxEvent e
            SET e.status = com.hims.ot.infrastructure.outbox.OutboxEvent.OutboxStatus.DLQ,
                e.attempts = :attempts
            WHERE e.id = :id
            """)
    int markDlq(@Param("id") long id, @Param("attempts") int attempts);
}
