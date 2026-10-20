package com.hims.item.domain.repository;

import com.hims.item.domain.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByUnitIdAndStatusTrueOrderByCodeAsc(Long unitId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence), same convention as ItemMasterService. */
    @Query(value = "SELECT nextval('ihms_item.\"supplier_seq\"')", nativeQuery = true)
    long nextSupplierId();
}
