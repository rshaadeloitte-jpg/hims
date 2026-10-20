package com.hims.item.domain.repository;

import com.hims.item.domain.model.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findByFromStoreIdOrderByAddedDatetimeDesc(Long fromStoreId);
    List<StockTransfer> findByToStoreIdOrderByAddedDatetimeDesc(Long toStoreId);
    List<StockTransfer> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_stock_transfer_seq\"')", nativeQuery = true)
    long nextTransferId();
}
