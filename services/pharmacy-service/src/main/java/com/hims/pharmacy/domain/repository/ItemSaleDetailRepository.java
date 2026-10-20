package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.model.ItemSaleDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemSaleDetailRepository extends JpaRepository<ItemSaleDetail, ItemSaleDetailKey> {

    List<ItemSaleDetail> findBySaleIdOrderByLineNoAsc(Long saleId);
}
