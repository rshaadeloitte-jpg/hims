package com.hims.item.domain.repository;

import com.hims.item.domain.model.PurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PurchaseOrderDetailRepository extends JpaRepository<PurchaseOrderDetail, Long> {
    List<PurchaseOrderDetail> findByPoId(Long poId);
    @Query(value = "SELECT nextval('ihms_item.t_purchase_order_detail_seq')", nativeQuery = true)
    long nextId();
}
