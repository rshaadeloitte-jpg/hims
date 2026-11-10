package com.hims.item.domain.repository;

import com.hims.item.domain.model.ItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemBatchRepository extends JpaRepository<ItemBatch, Long> {

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"item_batch_seq\"')", nativeQuery = true)
    long nextBatchId();
}
