package com.hims.item.domain.repository;
import com.hims.item.domain.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<PurchaseOrder> findByStoreIdOrderByAddedDatetimeDesc(Long storeId);
    List<PurchaseOrder> findBySupplierIdOrderByAddedDatetimeDesc(Long supplierId);
    @Query(value = "SELECT nextval('ihms_item.t_purchase_order_seq')", nativeQuery = true)
    long nextId();
}
