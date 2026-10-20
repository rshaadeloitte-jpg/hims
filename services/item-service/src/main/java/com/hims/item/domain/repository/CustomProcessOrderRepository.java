package com.hims.item.domain.repository;

import com.hims.item.domain.model.CustomProcessOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CustomProcessOrderRepository extends JpaRepository<CustomProcessOrder, Long> {
    List<CustomProcessOrder> findByStoreIdOrderByAddedDatetimeDesc(Long storeId);
    List<CustomProcessOrder> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);

    @Query(value = "SELECT nextval('ihms_item.t_custom_process_order_seq')", nativeQuery = true)
    long nextCpoId();
}
