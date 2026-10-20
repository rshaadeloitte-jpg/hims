package com.hims.radiology.domain.repository;

import com.hims.radiology.domain.model.RadOrderDetail;
import com.hims.radiology.domain.model.RadOrderDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RadOrderDetailRepository extends JpaRepository<RadOrderDetail, RadOrderDetailKey> {

    List<RadOrderDetail> findByRadOrderIdOrderByLineNoAsc(Long radOrderId);
}
