package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.NhcxRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NhcxRequestDetailRepository extends JpaRepository<NhcxRequestDetail, Long> {

    List<NhcxRequestDetail> findByRequestIdOrderByLineNoAsc(Long requestId);
}
