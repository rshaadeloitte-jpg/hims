package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodRequisitionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloodRequisitionDetailRepository extends JpaRepository<BloodRequisitionDetail, Long> {

    List<BloodRequisitionDetail> findByRequisitionId(Long requisitionId);
}
