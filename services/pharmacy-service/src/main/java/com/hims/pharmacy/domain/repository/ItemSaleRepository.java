package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.ItemSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemSaleRepository extends JpaRepository<ItemSale, Long> {

    /** List: optional status, patient and prescription filters, newest first. */
    @Query("""
            SELECT s FROM ItemSale s
            WHERE (:status IS NULL OR s.statusCode = :status)
              AND (:patientId IS NULL OR s.patientId = :patientId)
              AND (:prescriptionId IS NULL OR s.prescriptionId = :prescriptionId)
            ORDER BY s.saleId DESC
            """)
    List<ItemSale> search(@Param("status") Short status,
                          @Param("patientId") Long patientId,
                          @Param("prescriptionId") Long prescriptionId);

    /** Back-compat overload (no prescription filter). */
    default List<ItemSale> search(Short status, Long patientId) {
        return search(status, patientId, null);
    }

    /** One sale per prescription (WP-R2 rework): lookup existing sale by prescription. */
    Optional<ItemSale> findByPrescriptionId(Long prescriptionId);

    /** Same lookup but limited to one row, so it never throws on duplicate sales. */
    Optional<ItemSale> findFirstByPrescriptionId(Long prescriptionId);

    /** Same lookup, latest sale first, limited to one row (never throws on duplicates). */
    Optional<ItemSale> findFirstByPrescriptionIdOrderBySaleIdDesc(Long prescriptionId);

    /** Same lookup, top sale first, limited to one row (never throws on duplicates). */
    Optional<ItemSale> findTopByPrescriptionIdOrderBySaleIdDesc(Long prescriptionId);

    /** Existence check that never throws on duplicate sales. */
    boolean existsByPrescriptionId(Long prescriptionId);
}
