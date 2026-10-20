package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EqpPmSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface EqpPmScheduleRepository extends JpaRepository<EqpPmSchedule, Long> {

    /** All schedules of one asset — newest first (asset PM plans). */
    List<EqpPmSchedule> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /** Schedule-code lookup, case-insensitive (unique per partial index when present). */
    Optional<EqpPmSchedule> findByScheduleCodeIgnoreCase(String scheduleCode);

    /** Due list: active schedules whose next service has fallen due on or before {@code cutoff}. */
    List<EqpPmSchedule> findByStatusTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(Timestamp cutoff);

    /**
     * Schedule search. All filter params are gated by booleans and never bound
     * as null: PostgreSQL cannot infer the type of an untyped null parameter
     * (binds as bytea and breaks UPPER()/= comparisons), so callers pass
     * sentinel values that are ignored when the matching gate is false (same
     * convention as EquipmentRepository.search / EquipmentComplaintRepository.search).
     *
     * <p>Free-text search matches schedule_code.
     */
    @Query("""
            SELECT s FROM EqpPmSchedule s
            WHERE s.status = true
              AND (:hasUnit = false OR s.unitId = :unitId)
              AND (:hasEquipment = false OR s.equipmentId = :equipmentId)
              AND (:hasPmType = false OR s.pmType = :pmType)
              AND (:hasDueFrom = false OR s.nextDueDate >= :dueFrom)
              AND (:hasDueTo = false OR s.nextDueDate <= :dueTo)
              AND (:hasSearch = false
                   OR UPPER(s.scheduleCode) LIKE CONCAT('%', UPPER(:search), '%'))
            ORDER BY s.id DESC
            """)
    List<EqpPmSchedule> search(@Param("unitId") Long unitId,
                               @Param("hasUnit") boolean hasUnit,
                               @Param("equipmentId") Long equipmentId,
                               @Param("hasEquipment") boolean hasEquipment,
                               @Param("pmType") Long pmType,
                               @Param("hasPmType") boolean hasPmType,
                               @Param("dueFrom") Timestamp dueFrom,
                               @Param("hasDueFrom") boolean hasDueFrom,
                               @Param("dueTo") Timestamp dueTo,
                               @Param("hasDueTo") boolean hasDueTo,
                               @Param("search") String search,
                               @Param("hasSearch") boolean hasSearch);
}
