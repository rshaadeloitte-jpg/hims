package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.AdvanceLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AdvanceLedgerRepository extends JpaRepository<AdvanceLedger, Long> {

    /** Most recent entry for the patient (the running-balance row). */
    Optional<AdvanceLedger> findFirstByPatientIdOrderByAdvanceIdDesc(Long patientId);

    /** All RECEIPT rows for a patient, ascending by advance_id (FIFO order, BR-BILL-007). */
    @Query("""
            SELECT a FROM AdvanceLedger a
            WHERE a.patientId = :patientId
              AND a.entryType = com.hims.billing.domain.model.AdvanceLedger.TYPE_RECEIPT
            ORDER BY a.advanceId ASC
            """)
    List<AdvanceLedger> findReceiptsByPatientOrderByAdvanceIdAsc(@Param("patientId") Long patientId);

    /** Entries attached to one bill (for audit / cancellation lookups). */
    List<AdvanceLedger> findByBillIdOrderByAdvanceIdAsc(Long billId);

    /**
     * Live patient balance: sum of all RECEIPT amounts (credits) + CONSUMPTION
     * amounts (debits, already negative). Faster than reading the last
     * balance_after because PostgreSQL aggregates stream.
     */
    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM AdvanceLedger a
            WHERE a.patientId = :patientId
            """)
    BigDecimal sumBalanceByPatient(@Param("patientId") Long patientId);
}
