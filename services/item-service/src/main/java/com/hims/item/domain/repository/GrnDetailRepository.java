package com.hims.item.domain.repository;

import com.hims.item.domain.model.GrnDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GrnDetailRepository extends JpaRepository<GrnDetail, Long> {
    List<GrnDetail> findByGrnId(Long grnId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_grn_detail_seq\"')", nativeQuery = true)
    long nextGrnDetailId();
}
