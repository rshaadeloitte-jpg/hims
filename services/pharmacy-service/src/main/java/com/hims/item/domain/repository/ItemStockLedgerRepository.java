package com.hims.item.domain.repository;

import com.hims.item.domain.model.ItemStockLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemStockLedgerRepository extends JpaRepository<ItemStockLedger, Long> {

    List<ItemStockLedger> findByStoreIdAndItemIdOrderByAddedDatetimeDescIdDesc(Long storeId, Long itemId);
}
