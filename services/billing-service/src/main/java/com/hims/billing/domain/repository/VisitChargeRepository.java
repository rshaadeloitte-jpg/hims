package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.VisitCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitChargeRepository extends JpaRepository<VisitCharge, Long> {
    List<VisitCharge> findByDoctorIdOrderByVisitChargeIdAsc(Long doctorId);
    List<VisitCharge> findByDoctorIdAndVisitTypeOrderByVisitChargeIdAsc(Long doctorId, Long visitType);
    List<VisitCharge> findByServiceIdOrderByVisitChargeIdAsc(Long serviceId);
}
