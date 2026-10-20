package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.DoctorIncentive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorIncentiveRepository extends JpaRepository<DoctorIncentive, Long> {
    List<DoctorIncentive> findByDoctorIdOrderByIncentiveIdAsc(Long doctorId);
    List<DoctorIncentive> findByDoctorIdAndIsBilledOrderByIncentiveIdAsc(Long doctorId, Boolean isBilled);
}
