package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.NurseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NurseAllocationRepository extends JpaRepository<NurseAllocation, Long> {
    List<NurseAllocation> findByWardIdAndReleasedAtIsNull(Long wardId);
    List<NurseAllocation> findByNurseIdAndReleasedAtIsNull(Long nurseId);
    List<NurseAllocation> findByAdmissionIdAndReleasedAtIsNull(Long admissionId);
}
