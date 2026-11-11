package com.hims.item.domain.repository;

import com.hims.item.domain.model.CustomProcessOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CustomProcessOrderDetailRepository extends JpaRepository<CustomProcessOrderDetail, Long> {
    List<CustomProcessOrderDetail> findByCpoId(Long cpoId);

    @Query(value = "SELECT nextval('ihms_item.t_custom_process_order_detail_seq')", nativeQuery = true)
    long nextDetailId();
}
