package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.NurseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface NurseAllocationRepository extends JpaRepository<NurseAllocation, Long> {
    List<NurseAllocation> findByWardIdAndReleasedAtIsNull(Long wardId);
    List<NurseAllocation> findByNurseIdAndReleasedAtIsNull(Long nurseId);
    List<NurseAllocation> findByAdmissionIdAndReleasedAtIsNull(Long admissionId);

    @Query("SELECT a FROM NurseAllocation a WHERE a.wardId = :wardId " +
           "AND a.allocatedAt >= :startOfDay AND a.allocatedAt < :endOfDay")
    List<NurseAllocation> findByWardIdAndDate(@Param("wardId") Long wardId,
                                              @Param("startOfDay") Timestamp startOfDay,
                                              @Param("endOfDay") Timestamp endOfDay);
}
