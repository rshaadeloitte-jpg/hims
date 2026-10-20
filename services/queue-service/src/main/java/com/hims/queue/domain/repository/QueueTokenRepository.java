package com.hims.queue.domain.repository;

import com.hims.queue.domain.model.QueueToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    /**
     * Call-next ordering: EMERGENCY (2) jumps ahead of NORMAL (1), then FIFO
     * by daily token number. Legacy QMS pushToQ order + emergency priority.
     */
    @Query("""
            SELECT t FROM QueueToken t
            WHERE t.queueDate = :date AND t.departmentId = :departmentId
              AND t.queueStatus = :queueStatus AND t.status = true
            ORDER BY t.priority DESC, t.tokenNo ASC
            """)
    List<QueueToken> findNext(@Param("date") LocalDate date,
                              @Param("departmentId") Long departmentId,
                              @Param("queueStatus") short queueStatus,
                              Pageable pageable);

    /** Dashboard ordering across all departments (no dept filter). */
    @Query("""
            SELECT t FROM QueueToken t
            WHERE t.queueDate = :date AND t.queueStatus = :queueStatus AND t.status = true
            ORDER BY t.priority DESC, t.tokenNo ASC
            """)
    List<QueueToken> findByDateAndStatus(@Param("date") LocalDate date,
                                         @Param("queueStatus") short queueStatus,
                                         Pageable pageable);

    List<QueueToken> findByQueueDateAndStatusTrueOrderByIdAsc(LocalDate queueDate);

    List<QueueToken> findByQueueDateAndCounterIdAndStatusTrueOrderByIdAsc(LocalDate queueDate, Long counterId);

    List<QueueToken> findByQueueDateAndDepartmentIdAndStatusTrueOrderByIdAsc(LocalDate queueDate, Long departmentId);

    long countByQueueDateAndQueueStatus(LocalDate queueDate, short queueStatus);

    long countByQueueDateAndDepartmentIdAndQueueStatus(LocalDate queueDate, Long departmentId, short queueStatus);

    @Query("""
            SELECT t FROM QueueToken t
            WHERE t.queueDate = :date AND t.departmentId = :departmentId
              AND t.queueStatus = :queueStatus AND t.status = true
            ORDER BY t.priority DESC, t.tokenNo ASC
            """)
    List<QueueToken> findByDateDeptAndStatus(@Param("date") LocalDate date,
                                             @Param("departmentId") Long departmentId,
                                             @Param("queueStatus") short queueStatus,
                                             Pageable pageable);
}
