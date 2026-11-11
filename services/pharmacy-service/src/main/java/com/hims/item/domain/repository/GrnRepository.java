package com.hims.item.domain.repository;

import com.hims.item.domain.model.Grn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GrnRepository extends JpaRepository<Grn, Long> {
    List<Grn> findByDocStatus(Short docStatus);
    List<Grn> findByStoreIdOrderByAddedDatetimeDescIdDesc(Long storeId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_grn_seq\"')", nativeQuery = true)
    long nextGrnId();
}
