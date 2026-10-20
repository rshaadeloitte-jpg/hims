package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.ItemSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemSaleRepository extends JpaRepository<ItemSale, Long> {

    /** List: optional status and patient filters, newest first. */
    @Query("""
            SELECT s FROM ItemSale s
            WHERE (:status IS NULL OR s.statusCode = :status)
              AND (:patientId IS NULL OR s.patientId = :patientId)
            ORDER BY s.saleId DESC
            """)
    List<ItemSale> search(@Param("status") Short status, @Param("patientId") Long patientId);
}
