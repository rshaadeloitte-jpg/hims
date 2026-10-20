package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietOrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface DietOrderDetailsRepository extends JpaRepository<DietOrderDetails, Long> {

    /**
     * Legacy getDietOrderDetailByAdmissionId: newest ACTIVE (orderStatus=2)
     * diet order for an admission in a unit.
     */
    @Query("""
            SELECT d FROM DietOrderDetails d
            WHERE d.admissionId = :admissionId
              AND d.unitId = :unitId
              AND d.orderStatus = 2
              AND d.status = true
            ORDER BY d.id DESC
            LIMIT 1
            """)
    Optional<DietOrderDetails> findLatestActiveByAdmission(@Param("admissionId") Long admissionId,
                                                           @Param("unitId") Long unitId);

    /**
     * Legacy addPatDietOrder pre-check: the ACTIVE (orderStatus=2) diet order
     * for a patient in a unit (first match; legacy used .get()).
     */
    @Query("""
            SELECT d FROM DietOrderDetails d
            WHERE d.patientId = :patientId
              AND d.unitId = :unitId
              AND d.orderStatus = 2
              AND d.status = true
            ORDER BY d.id DESC
            LIMIT 1
            """)
    Optional<DietOrderDetails> findActiveByPatient(@Param("patientId") Long patientId,
                                                   @Param("unitId") Long unitId);

    /** Legacy updateDietOrderDetails: all rows sharing the business orderId field. */
    @Query("""
            SELECT d FROM DietOrderDetails d
            WHERE d.orderId = :orderId
              AND d.status = true
            ORDER BY d.id DESC
            """)
    List<DietOrderDetails> findByOrderId(@Param("orderId") Long orderId);

    /** Legacy getPatientDietOrderList: all orders for an admission, newest first. */
    @Query("""
            SELECT d FROM DietOrderDetails d
            WHERE d.admissionId = :admissionId
              AND d.unitId = :unitId
              AND d.status = true
            ORDER BY d.id DESC
            """)
    List<DietOrderDetails> findByAdmission(@Param("admissionId") Long admissionId,
                                           @Param("unitId") Long unitId);

    /**
     * Legacy getFilterwiseDietOrderList: admission-scoped orders with an
     * optional fromDate window (legacy: fromDate &gt; :fromDate AND fromDate
     * &lt; :toDate — strict bounds preserved). Null-sentinel gated.
     */
    @Query("""
            SELECT d FROM DietOrderDetails d
            WHERE d.admissionId = :admissionId
              AND d.unitId = :unitId
              AND d.status = true
              AND (:hasRange = false
                   OR (d.fromDate > :fromDate AND d.fromDate < :toDate))
            ORDER BY d.id DESC
            """)
    List<DietOrderDetails> findFiltered(@Param("admissionId") Long admissionId,
                                        @Param("unitId") Long unitId,
                                        @Param("fromDate") Timestamp fromDate,
                                        @Param("toDate") Timestamp toDate,
                                        @Param("hasRange") boolean hasRange);
}
