package com.hims.item.domain.repository;

import com.hims.item.domain.model.StockTransferDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockTransferDetailRepository extends JpaRepository<StockTransferDetail, Long> {
    List<StockTransferDetail> findByTransferId(Long transferId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_stock_transfer_detail_seq\"')", nativeQuery = true)
    long nextDetailId();
}
