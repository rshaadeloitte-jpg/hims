package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietItemScheduleDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DietItemScheduleDetailsRepository extends JpaRepository<DietItemScheduleDetails, Long> {

    /**
     * Legacy replace query (addPatDietOrder / addPatFooddItemOrder): active,
     * not-yet-completed (itemScheduleStatus &lt; 6) schedule rows for an
     * admission + order, optionally one session, on a given food-ordered day.
     * hasSession=false matches any session (legacy addPatDietOrder variant).
     */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.admissionId = :admissionId
              AND s.orderId = :orderId
              AND s.unitId = :unitId
              AND s.status = true
              AND s.itemScheduleStatus < 6
              AND (:hasSession = false OR s.sessionId = :sessionId)
              AND (:hasDay = false OR cast(s.foodOrderedDate as LocalDate) = :day)
            ORDER BY s.id DESC
            """)
    List<DietItemScheduleDetails> findReplaceable(@Param("admissionId") Long admissionId,
                                                  @Param("orderId") Long orderId,
                                                  @Param("unitId") Long unitId,
                                                  @Param("sessionId") Long sessionId,
                                                  @Param("hasSession") boolean hasSession,
                                                  @Param("day") LocalDate day,
                                                  @Param("hasDay") boolean hasDay);

    /**
     * Legacy "already completed today" check (addPatFooddItemOrder): active
     * COMPLETED (itemScheduleStatus = 6) rows for an admission + session on a
     * given food-ordered day.
     */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.admissionId = :admissionId
              AND s.sessionId = :sessionId
              AND s.unitId = :unitId
              AND s.status = true
              AND s.itemScheduleStatus = 6
              AND cast(s.foodOrderedDate as LocalDate) = :day
            ORDER BY s.id DESC
            LIMIT 1
            """)
    Optional<DietItemScheduleDetails> findCompletedToday(@Param("admissionId") Long admissionId,
                                                         @Param("sessionId") Long sessionId,
                                                         @Param("unitId") Long unitId,
                                                         @Param("day") LocalDate day);

    /**
     * Legacy updateDietFoodOrder / updateUncheckFoodOrder /
     * updateDietFoodStatusForCancel composite key: active row matching
     * (admissionId, sessionId, orderId, id) on a given food-ordered day.
     */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.admissionId = :admissionId
              AND s.sessionId = :sessionId
              AND s.orderId = :orderId
              AND s.id = :id
              AND s.status = true
              AND (:hasDay = false OR cast(s.foodOrderedDate as LocalDate) = :day)
            """)
    Optional<DietItemScheduleDetails> findByCompositeKey(@Param("admissionId") Long admissionId,
                                                         @Param("sessionId") Long sessionId,
                                                         @Param("orderId") Long orderId,
                                                         @Param("id") Long id,
                                                         @Param("day") LocalDate day,
                                                         @Param("hasDay") boolean hasDay);

    /**
     * Legacy removeDietItem key: (unitId, admissionId, sessionId, id).
     */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.unitId = :unitId
              AND s.admissionId = :admissionId
              AND s.sessionId = :sessionId
              AND s.id = :id
            """)
    Optional<DietItemScheduleDetails> findForRemove(@Param("unitId") Long unitId,
                                                    @Param("admissionId") Long admissionId,
                                                    @Param("sessionId") Long sessionId,
                                                    @Param("id") Long id);

    /**
     * Legacy getDietKichenDetails diet-owned half: active rows at or past the
     * kitchen stage (itemScheduleStatus &gt;= 3) for a unit on a given
     * food-ordered day, optionally one session. Ward/service decoration JOINs
     * are deferred (cross-domain).
     */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.unitId = :unitId
              AND s.status = true
              AND s.itemScheduleStatus >= 3
              AND (:hasDay = false OR cast(s.foodOrderedDate as LocalDate) = :day)
              AND (:hasSession = false OR s.sessionId = :sessionId)
            ORDER BY s.id ASC
            """)
    List<DietItemScheduleDetails> findKitchenList(@Param("unitId") Long unitId,
                                                  @Param("day") LocalDate day,
                                                  @Param("hasDay") boolean hasDay,
                                                  @Param("sessionId") Long sessionId,
                                                  @Param("hasSession") boolean hasSession);

    /** Active schedule rows of one diet order, ordered by session then id. */
    @Query("""
            SELECT s FROM DietItemScheduleDetails s
            WHERE s.orderId = :orderId
              AND s.status = true
            ORDER BY s.sessionId ASC, s.id ASC
            """)
    List<DietItemScheduleDetails> findByOrder(@Param("orderId") Long orderId);
}
